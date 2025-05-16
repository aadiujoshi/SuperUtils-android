package com.example.superutils.data

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.content.FileProvider
import com.example.superutils.SuperUtilsApp
import com.example.superutils.network.StatusHolder
import com.example.superutils.network.SuperParcel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
import java.util.UUID

data class StoredFile(
    val fileName: String,
    val mimeType: String
)

class StorageManager {
    val TAG: String = "Storage Manager"

    companion object {
        val instance = StorageManager()
    }

    private var _internalUpdateKey = MutableStateFlow(0)
    val storageUpdateKey: StateFlow<Int> = _internalUpdateKey;

    private fun updateStorageKey() {
        Log.d(TAG, "UPDATED STORAGE KEY ${_internalUpdateKey.value}")
        _internalUpdateKey.value += 1
    }

    fun saveIncomingSuperParcel(superParcel: SuperParcel): List<Result<StoredFile>> {
        val result = superParcel.getAllItems().map { parcel ->
            parcel.getBytes()?.let { bytes ->
                saveDataFromByteArray(bytes, parcel.mimeType)
            } ?: Result.Failure("Parcel had no data bytes")
        }
        updateStorageKey();
        return result;
    }

    fun saveDataFromByteArray(
        data: ByteArray,
        mimeType: String,
    ): Result<StoredFile> {
        val TAG = "SaveData"
        return try {
            val bytesToWrite = if (mimeType == "text/plain") {
                Log.d(TAG, "Mime type is text/plain, ensuring UTF-8 encoding.")
                String(data, Charsets.UTF_8).toByteArray(Charsets.UTF_8)
            } else {
                Log.d(TAG, "Mime type is $mimeType, writing raw bytes.")
                data
            }

            val dir = getDirectoryForMimeType(SuperUtilsApp.appContext, mimeType)
            if (!dir.exists()) {
                Log.d(TAG, "Directory does not exist. Creating: ${dir.absolutePath}")
                dir.mkdirs()
            } else {
                Log.d(TAG, "Directory exists: ${dir.absolutePath}")
            }

            val extension = MimeHelper.getExtension(mimeType)
            val fileName = "${System.currentTimeMillis()}_${UUID.randomUUID()}$extension"
            val file = File(dir, fileName)
            Log.d(TAG, "Saving file as: ${file.absolutePath}")

            FileOutputStream(file).use { it.write(bytesToWrite) }

            Log.d(TAG, "File saved successfully.")
            updateStorageKey();
            Result.Success(StoredFile(fileName, mimeType))
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save file: ${e.message}", e)
            Result.Failure("Failed to save file: ${e.message}")
        }
    }

    fun listFilesByType(type: String, context: Context): List<File> {
        val TAG = "ListFiles"

        val dir = File(context.filesDir, type)
        Log.d(TAG, "Looking in directory: ${dir.absolutePath}")

        val files = dir.listFiles()
        if (files != null) {
            Log.d(TAG, "Found ${files.size} file(s): ${files.toList()}")
        } else {
            Log.d(TAG, "No files found or directory does not exist.")
        }

        return files?.toList() ?: emptyList()
    }

    fun deleteFileByName(fileName: String, mimeType: String, context: Context): Boolean {
        val file = File(context.filesDir, "${getFolderForMimeType(mimeType)}/$fileName")
        if (file.exists() && file.delete()){
            updateStorageKey();
            return true;
        }
        return false;
    }

    fun getFile(context: Context, fileName: String, type: String): Result<File> {
        val file = File(context.filesDir, "$type/$fileName")
        return if (file.exists()) Result.Success(file) else Result.Failure("File not found")
    }

    private fun getFolderForMimeType(mimeType: String): String {
        return when {
            mimeType.startsWith("image") -> "images"
            mimeType.startsWith("video") -> "videos"
            mimeType.startsWith("text") -> "text"
            else -> "others"
        }
    }

    private fun getDirectoryForMimeType(context: Context, mimeType: String): File {
        val folder = getFolderForMimeType(mimeType)
        return File(context.filesDir, folder)
    }

    fun renameFile(context: Context, newFileName: String, oldFileName: String, mimeType: String): Result<File> {
        val TAG = "RenameFile"
        val dir = getDirectoryForMimeType(context, mimeType)
        val oldFile = File(dir, oldFileName)

        if (!oldFile.exists()) {
            Log.e(TAG, "File $oldFileName does not exist in ${dir.absolutePath}")
            return Result.Failure("File $oldFileName does not exist.")
        }

        val extension = MimeHelper.getExtension(mimeType)
        val newFile = File(dir, if (newFileName.endsWith(extension)) newFileName else "$newFileName$extension")

        if (newFile.exists()) {
            Log.e(TAG, "A file with name $newFileName already exists in ${dir.absolutePath}")
            return Result.Failure("A file with name $newFileName already exists.")
        }

        return if (oldFile.renameTo(newFile)) {
            Log.d(TAG, "Renamed $oldFileName to ${newFile.name}")
            updateStorageKey();
            Result.Success(newFile)
        } else {
            Log.e(TAG, "Failed to rename file $oldFileName to $newFileName")
            Result.Failure("Failed to rename file.")
        }
    }
}
