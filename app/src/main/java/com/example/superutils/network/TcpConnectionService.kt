package com.example.superutils.network

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.example.superutils.data.CharSequenceTracker
import com.example.superutils.data.Result
import com.example.superutils.data.StorageManager
import com.example.superutils.data.isFailure
import com.example.superutils.data.print
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

data class StatusHolder(val good: Boolean, val desc: String, var color: Color) {
    companion object {
        fun good(description: String) = StatusHolder(good = true, desc = description, color = Color.Green)
        fun bad(description: String) = StatusHolder(good = false, desc = description, color = Color.Red)
        fun ok(description: String) = StatusHolder(good = false, desc = description, color = Color.Blue)
    }
}

class TcpConnectionService {
    companion object {
        val instance = TcpConnectionService()
        val NO_PARCEL_STREAM = StatusHolder.bad("No open parcel stream")
        val REFRESHING_CONNECTION = StatusHolder.bad("Refreshing connection...")
        val CONNECTED_TO_CLIENT = StatusHolder.good("Connected to client")
        val WAITING_FOR_CLIENT = StatusHolder.ok("Waiting for client")
        val IDLE_PARCEL_STREAM = StatusHolder.ok("No incoming data")
        val SENDING_PARCEL = StatusHolder.ok("Sending data...")
        val FAILED_TO_SEND_PARCEL = StatusHolder.bad("Failed to send recent")
        val FAILED_TO_RECEIVE_PARCEL = StatusHolder.bad("Failed to receive recent")
        val RECEIVING_PARCEL = StatusHolder.ok("Receiving data...")
        val TAG = "TcpServer"
    }

    @Volatile
    private var isDoingOperation = false
    @Volatile
    private var killed = true;

    private var acceptClientExecutor = Executors.newSingleThreadExecutor()
    private var receiveLoopExecutor = Executors.newSingleThreadExecutor()

    @Volatile
    private var clientSocket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private val _connectionStatus = MutableStateFlow(WAITING_FOR_CLIENT)
    val connectionStatus: StateFlow<StatusHolder> = _connectionStatus

    private val _parcelStatus = MutableStateFlow(StatusHolder.bad("No open parcel stream"))
    val parcelStatus: StateFlow<StatusHolder> = _parcelStatus

    private val _parcelProgress = MutableStateFlow(0.0)
    val parcelProgress: StateFlow<Double> = _parcelProgress

    private fun updateConnectionStatus(status: StatusHolder) {
        _connectionStatus.value = status
    }

    private fun updateParcelStatus(status: StatusHolder) {
        _parcelStatus.value = status
    }

    private fun updateParcelProgress(status: Double) {
        _parcelProgress.value = status
    }

    init {
        openPortAndAcceptClient()
    }

    private fun openPortAndAcceptClient() {
        if (isDoingOperation) return
        isDoingOperation = true

        // Recreate executor if it was shut down
        if (acceptClientExecutor.isShutdown) {
            acceptClientExecutor = Executors.newSingleThreadExecutor()
            Log.d(TAG, "Recreated acceptClientExecutor")
        }

        acceptClientExecutor.execute {
            try {
                // Clean up old server socket if needed
                serverSocket?.close()
                Log.d(TAG, "Closed old server socket if it existed")

                serverSocket = ServerSocket(8888)
                updateConnectionStatus(WAITING_FOR_CLIENT)
                Log.i(TAG, "TCP Server listening on port 8888")

                // Accept new client
                clientSocket?.close()
                Log.d(TAG, "Closed old client socket if it existed")

                clientSocket = serverSocket!!.accept()
                val clientAddress = clientSocket!!.inetAddress.hostAddress
                Log.i(TAG, "Accepted connection from $clientAddress")
                updateConnectionStatus(StatusHolder.good("Connected to client: $clientAddress"))
                updateParcelStatus(IDLE_PARCEL_STREAM)

                // Recreate and run receive loop if needed
                if (receiveLoopExecutor.isShutdown) {
                    receiveLoopExecutor = Executors.newSingleThreadExecutor()
                    Log.d(TAG, "Recreated receiveLoopExecutor")
                }

                receiveLoopExecutor.execute {
                    Log.d(TAG, "Starting receiveParcelLoop()")
                    receiveParcelLoop()
                }
                killed = false;

            } catch (e: Exception) {
                Log.e(TAG, "Error in openPortAndAcceptClient: ${e.message}", e)
                updateConnectionStatus(StatusHolder.bad("Failed at openPortAndAcceptClient: ${e.message}"))
                updateParcelStatus(NO_PARCEL_STREAM)
            } finally {
                isDoingOperation = false
                Log.d(TAG, "Finished openPortAndAcceptClient execution")
            }
        }
    }

    suspend fun sendParcelAsync(parcelData: ByteArray): Result<Any> = withContext(Dispatchers.IO) {
        if (killed) {
            return@withContext Result.Failure("Tcp instance is dead")
        }

        if (isDoingOperation) {
            return@withContext Result.Failure("Already doing another operation")
        }

        if (getConnectionState().isFailure) {
            return@withContext Result.Failure("Not connected")
        }

        isDoingOperation = true;

        try {
            updateParcelStatus(SENDING_PARCEL)

            val outputStream = clientSocket!!.getOutputStream()

            var sent = 0
            val total = parcelData.size
            val chunkSize = 1024

            while (sent < total) {
                //force stopped by something else
                if (!isDoingOperation){
                    return@withContext Result.Failure("Interrupted by refresh while sending")
                }

                val remaining = total - sent
                val toSend = if (remaining > chunkSize) chunkSize else remaining

                outputStream.write(parcelData, sent, toSend)
                sent += toSend

                val progress = sent.toDouble() / total
                updateParcelProgress(progress)
            }

            outputStream.flush()

            updateParcelStatus(StatusHolder.good("Successfully Sent: bytes=$total"))
            return@withContext Result.Success("Successfully sent data: bytes=$total")
        } catch (e: Exception) {
            updateParcelStatus(FAILED_TO_SEND_PARCEL)
            forceRefresh()
            return@withContext Result.Failure("Error occurred while sending: ${e.message}")
        } finally {
            isDoingOperation = false;
        }
    }

    private fun receiveParcelLoop() {
        try {
            val input: InputStream = clientSocket!!.getInputStream()

            while (true) {
                if (getConnectionState().isFailure){
                    Log.d(TAG, "Inner Receive parcel loop error:::")
                    getConnectionState().print
                    forceRefresh()
                }

                try {
                    receiveParcelAsync(input)
                } catch (e: Exception) {
                    e.printStackTrace()
                    break
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Receive parcel loop error:" + e.message)

        }
    }

    private fun receiveParcelAsync(input: InputStream) {
        val endFlagTracker = CharSequenceTracker("END_OF_TRANSMISSION".toByteArray(Charsets.UTF_8))
        val buffer = ByteArray(4096)
        val collectedBytes = ByteArrayOutputStream()

        outer@ while (true) {
            val bytesRead = input.read(buffer)

//            if (getConnectionState().isFailure){
//                getConnectionState().print
//                //will be handled by the outer loop
//                return;
//            }

            if (bytesRead < 0) {
                Thread.sleep(100)
                continue@outer
            }

            collectedBytes.write(buffer, 0, bytesRead)

            updateParcelProgress(parcelProgress.value + 1)
            updateParcelStatus(RECEIVING_PARCEL)

            for (i in 0 until bytesRead) {
                if (endFlagTracker.found()) {
                    Log.d(TAG, "End flag found!")
                    break@outer
                }
                endFlagTracker.nextChar(buffer[i])
            }
        }

        val parcelBytes = collectedBytes.toByteArray()

        when (val result = SuperParcel.fromParcelBytes(parcelBytes)) {
            is Result.Success -> {
                updateParcelStatus(StatusHolder.good("Successfully Received Data"))
                updateParcelProgress(1.0)

                StorageManager.instance.saveIncomingSuperParcel(result.data)
                println("Parsed parcel successfully: ${result.data.getAllItems().size} items")
            }

            is Result.Failure -> {
                updateParcelStatus(FAILED_TO_SEND_PARCEL)
                updateParcelProgress(0.0)

                println("Parcel parse failed: ${result.error}")
            }
        }
    }

    fun refresh(): Result<Any> {
        if (isDoingOperation){
            return Result.Failure("Failed to refresh, already doing something");
        }
        updateConnectionStatus(REFRESHING_CONNECTION);
        updateParcelStatus(NO_PARCEL_STREAM);

        dispose()
        openPortAndAcceptClient()
        return Result.Success("Successfully tried to refresh")
    }

    fun forceRefresh(): Result<Any> {
        isDoingOperation = false;
        return refresh();
    }

    fun dispose() {
        if (isDoingOperation){
            return
        }

        Log.d(TAG, "Disposing TCP connection...")

        killed = true
        isDoingOperation = true;

        acceptClientExecutor.shutdownNow()
        Log.d(TAG, "Accept executor shut down.")

        receiveLoopExecutor.shutdownNow()
        Log.d(TAG, "Receive loop executor shut down.")

        try {
            clientSocket?.close()
            Log.d(TAG, "Client socket closed.")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing client socket", e)
        }
        clientSocket = null

        try {
            serverSocket?.close()
            Log.d(TAG, "Server socket closed.")
        } catch (e: IOException) {
            Log.e(TAG, "Error closing server socket", e)
        }
        serverSocket = null

        updateConnectionStatus(StatusHolder.bad("Disposed"))
        updateParcelStatus(StatusHolder.bad("Disposed"))
        updateParcelProgress(0.0)

        isDoingOperation = false;

        Log.i(TAG, "Disposed successfully.")
    }

    private fun getConnectionState(): Result<Any> {
        if (serverSocket == null) {
            return Result.Failure("Null server socket");
        }
        if (serverSocket!!.isClosed) {
            return Result.Failure("Server socket was closed");
        }
        if (clientSocket == null) {
            return Result.Failure("Null client socket");
        }

        return try {
            if (clientSocket!!.isClosed || !clientSocket!!.isConnected) return Result.Failure("")

            val input = clientSocket!!.getInputStream()
            if (input.available() > 0) {
                val peek = input.read()
                if (peek == -1) return Result.Failure("Read nothing from stream")
            }
            return Result.Success(true);
        } catch (e: IOException) {
            Result.Failure("Dead socket: $e") // socket error indicates dead connection
        }
    }
}


//SINGLE FUNCTION
//private fun receiveParcelAsyncLoop() {
//    var socketIsDead = true
//
//    try {
//        var input: InputStream = ByteArrayInputStream(ByteArray(0))
//        while (true) {
//            if (getSocketState().isFailure) {
//                socketIsDead = true
//                Thread.sleep(100)
//                continue
//            } else if (socketIsDead && clientSocket != null) {
//                socketIsDead = false
//                input = clientSocket!!.getInputStream()
//            }
//
//            try {
//                val endFlagTracker = CharSequenceTracker("END_OF_TRANSMISSION".toByteArray(Charsets.UTF_8))
//
//                val buffer = ByteArray(4096)
//
//                val collectedBytes = ByteArrayOutputStream()
//                outer@ while (true) {
//                    val bytesRead = input.read(buffer)
//
//                    collectedBytes.write(buffer, 0, bytesRead)
//
//                    for (i in 0 until bytesRead) {
//                        if (endFlagTracker.found()){
//                            Log.d(TAG, "End flag found!")
//                            break@outer
//                        }
//                        endFlagTracker.nextChar(buffer[i])
//                    }
//                }
//
//                val parcelBytes = collectedBytes.toByteArray()
//
//                when (val result = SuperParcel.fromParcelBytes(parcelBytes)) {
//                    is Result.Success -> {
//                        updateReceivedParcel(result.data)
//                        println("Parsed parcel successfully: ${result.data.getAllItems().size} items")
//                    }
//                    is Result.Failure -> {
//                        println("Parcel parse failed: ${result.error}")
//                    }
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                break
//            }
//        }
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//}


//private val _receivedParcel = MutableStateFlow<SuperParcel?>(null)
//val receivedParcel: StateFlow<SuperParcel?> = _receivedParcel

//private fun updateReceivedParcel(result: SuperParcel) {
//    Log.d(TAG, "udapteing received parcel flow state")
//    _receivedParcel.value = result
//}
