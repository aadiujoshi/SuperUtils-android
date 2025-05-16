package com.example.superutils.network

import android.graphics.Bitmap
import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import com.example.superutils.data.CharSequenceTracker
import com.example.superutils.data.MimeHelper
import com.example.superutils.data.Result
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

class SuperParcel {
    //provided raw data should never be encoded. if it was encoded, must be decoded to bytes
    data class ParcelItem(val mimeType: String, val rawData: Any) {
        fun getBytes(): ByteArray? {
            return convertDataToBytes(this.rawData)
        }
    }

    private val items = mutableStateListOf<ParcelItem>()

    fun clear() {
        items.clear()
    }

    fun addItem(parcelItem: ParcelItem) {
        this.addItem(parcelItem.mimeType, parcelItem.rawData)
    }

    fun addItem(mimeType: String?, rawData: Any?) {
        if (mimeType.isNullOrEmpty() || rawData == null) {
            Log.d(TAG, "addItem: Skipping because mimeType or data is null or empty")
            return
        }

        Log.d(TAG, "addItem: mimeType = $mimeType, data class = ${rawData::class.simpleName}")

        items.add(ParcelItem(mimeType, rawData))
        Log.d(TAG, "addItem: Item added. Total items now = ${items.size}")
    }

    fun getAllItems(): List<ParcelItem> {
        return items.map { ParcelItem(it.mimeType, it.rawData) }
    }

    private fun getBodyByteParcel(): ByteArray {
        val bodyStream = ByteArrayOutputStream()

        for (item in items) {
            bodyStream.write("--ITEM_START--".toByteArray())
            bodyStream.write("MIMETYPE:${item.mimeType}".toByteArray())
            bodyStream.write("CONTENT:".toByteArray())
            bodyStream.write(item.getBytes()) // Write raw bytes
            bodyStream.write("--ITEM_END--\n".toByteArray())
        }
        bodyStream.write("END_OF_TRANSMISSION\n".toByteArray())

        return bodyStream.toByteArray()
    }

    fun getFullByteParcel(): ByteArray {
        val bodyBytes = this.getBodyByteParcel()
        val checksum = computeSHA256(bodyBytes) // must accept ByteArray

        //------------------------------------------------------------------------------------
        //                             HEADER
        //------------------------------------------------------------------------------------
        val headerBuilder = StringBuilder()
        headerBuilder.appendLine("[SUPERUTILS_PARCEL]")
        headerBuilder.appendLine("VERSION:1")
        headerBuilder.appendLine("TOTAL_SIZE:${bodyBytes.size}")
        headerBuilder.appendLine("NUM_ITEMS:${items.size}")
        headerBuilder.appendLine("CHECKSUM:$checksum")
        headerBuilder.appendLine("END_HEADER")

        val headerBytes = headerBuilder.toString().toByteArray()

        // Combine header + body
        val finalStream = ByteArrayOutputStream()
        finalStream.write(headerBytes)
        finalStream.write(bodyBytes)

        return finalStream.toByteArray()
    }

    companion object {

        val TAG = "SuperParcel"

        fun fromParcelBytes(parcelBytes: ByteArray): Result<SuperParcel> {
            fun copyTillCharSequence(
                srcStart: Int, src: ByteArray, dest: ByteArrayOutputStream, sequence: String
            ): Int {
                val sequenceTracker = CharSequenceTracker(sequence.toByteArray())

                var findIndex = srcStart
                while (!sequenceTracker.found()) {
                    sequenceTracker.nextChar(src[findIndex], findIndex)
                    findIndex += 1
                }

                for (i in srcStart..<sequenceTracker.relativeStart) {
                    dest.write(src[i].toInt())
                }

                return sequenceTracker.relativeStart - 1
            }

            val parsed = SuperParcel()

            val checksumFromParcel = ByteArrayOutputStream()
            val currentMimeType = ByteArrayOutputStream()
            val currentParcelItemContent = ByteArrayOutputStream()

            var i = 0
            val checksumSequence = CharSequenceTracker("CHECKSUM:".toByteArray())
            val itemStartSequence = CharSequenceTracker("--ITEM_START--".toByteArray())
            val mimeTypeSequence = CharSequenceTracker("MIMETYPE:".toByteArray())
            val contentSequence = CharSequenceTracker("CONTENT:".toByteArray())
            val endTransmissionSequence = CharSequenceTracker("END_OF_TRANSMISSION\n".toByteArray())

            while (i < parcelBytes.size) {
                val byte = parcelBytes[i];

                if (checksumSequence.found()) {
                    i = copyTillCharSequence(i, parcelBytes, checksumFromParcel, "\nEND_HEADER")
                    checksumSequence.markComplete()
                } else {
                    checksumSequence.nextChar(byte, i)
                }

                if (itemStartSequence.found()) {
                    if (mimeTypeSequence.found()) {
                        i = copyTillCharSequence(i, parcelBytes, currentMimeType, "CONTENT:")
                        mimeTypeSequence.markComplete()
                    } else {
                        mimeTypeSequence.nextChar(byte, i)
                    }

                    if (contentSequence.found()) {
                        i = copyTillCharSequence(
                            i, parcelBytes, currentParcelItemContent, "--ITEM_END--"
                        )

                        //strings should be saved as utf8 encoded strings at all times
                        val mimeType = String(currentMimeType.toByteArray())
                        val content = currentParcelItemContent.toByteArray()
                        if (mimeType == MimeHelper.getMimeType(MimeHelper.TXT)) {
                            parsed.addItem(mimeType, String(content))
                        } else {
                            parsed.addItem(mimeType, content)
                        }

                        itemStartSequence.reset()
                        contentSequence.reset()
                        mimeTypeSequence.reset()

                        currentMimeType.reset()
                        currentParcelItemContent.reset()
                    } else {
                        contentSequence.nextChar(byte, i)
                    }
                } else {
                    itemStartSequence.nextChar(byte, i)
                }

                if (endTransmissionSequence.found()) {
                    break
                } else {
                    endTransmissionSequence.nextChar(byte, i)
                }

                i++
            }

            if (!endTransmissionSequence.found()) {
                return Result.Failure("Missing end of transmission")
            }

            //both are strings, which when printed are the same, but are returning as not equal, can you print the byte array list for each
            val hashed = computeSHA256(parsed.getBodyByteParcel()).substring(0, 64)
            val parcelChecksumString = String(checksumFromParcel.toByteArray(), Charsets.UTF_8).substring(0, 64)

            if (hashed != parcelChecksumString) {
                // Print as hex list for both

//                println("Hashed (computed): ${hashed.toByteArray().joinToString()}|${hashed.toByteArray().size}")
//                println("Parcel Checksum String (from bytes): ${checksumFromParcel.toByteArray().joinToString()}|${checksumFromParcel.toByteArray().size}")

                // Also show plain strings surrounded by delimiters
//                println("|$hashed|")
//                println("|$parcelChecksumString|")

                return Result.Failure("Mismatched SHA256 checksum: |$hashed|$parcelChecksumString|")
            }


            return Result.Success(parsed)
        }

        private fun computeSHA256(input: ByteArray): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(input)
            return hash.joinToString("") { "%02x".format(it) }.uppercase()
        }

        private fun convertDataToBytes(data: Any): ByteArray? {
            return when (data) {
                is String -> {
                    val file = File(data)
                    if (file.exists()) {
                        // If it's a file path, read the file as bytes
                        file.readBytes()
                    } else {
                        // Otherwise, treat it as a UTF-8 encoded string
                        data.toByteArray(Charsets.UTF_8)
                    }
                }

                is File -> {
                    if (data.exists()) {
                        data.readBytes()
                    } else {
                        throw Exception("File does not exist: ${data.absolutePath}")
                    }
                }

                is Bitmap -> {
                    val stream = ByteArrayOutputStream()
                    data.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    stream.toByteArray()
                }

                is ByteArray -> {
                    data
                }

                else -> {
                    throw Exception("Unrecognized data type: $data + ${data.javaClass.simpleName}")
                }
            }
        }
    }
}


//            Log.d(TAG, "computed:$hashed|parcel:${String(checksumFromParcel.toByteArray(), Charsets.UTF_8)}|")