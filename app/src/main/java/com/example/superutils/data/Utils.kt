package com.example.superutils.data

import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.example.superutils.network.SuperParcel
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.net.URL


object UriUtils {
    // Utility function to copy file from URI to cache and return a ParcelItem
    fun copyUriToCacheAndCreateParcelItem(context: Context, uri: Uri): SuperParcel.ParcelItem {
        // Get the filename from URI
        val fileName = getFileNameFromUri(context, uri)
            ?: throw IllegalArgumentException("Invalid file: filename not found")

        // Copy the file from URI to cache directory
        val copiedFile = copyUriToCache(context, uri, fileName)

        // Get the MIME type using the file extension
        val mimeType = MimeHelper.getMimeType(fileName)

        // Create a ParcelItem from the copied file
        return SuperParcel.ParcelItem(mimeType, copiedFile)
    }

    // Get the file name from URI
    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (it.moveToFirst()) {
                return it.getString(nameIndex)
            }
        }
        return null
    }

    // Copy the file from URI to app's cache directory
    private fun copyUriToCache(context: Context, uri: Uri, fileName: String): File {
        val cacheFile = File(context.cacheDir, fileName)
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Unable to open input stream for URI: $uri")
        val outputStream: OutputStream = FileOutputStream(cacheFile)

        inputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }

        return cacheFile
    }
}

object ClipboardUtils {

    fun copyFileToClipboard(context: Context, file: File) {
        val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

        when {
            file.extension.equals("txt", ignoreCase = true) -> {
                val text = file.readText()
                val clip = ClipData.newPlainText("File Content", text)
                clipboardManager.setPrimaryClip(clip)
            }
            file.extension.equals("jpg", ignoreCase = true) ||
                    file.extension.equals("jpeg", ignoreCase = true) ||
                    file.extension.equals("png", ignoreCase = true) -> {
                copyImageToClipboard(context, file)
            }
            else -> {
                Toast.makeText(context, "Cannot copy this file type.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun copyImageToClipboard(context: Context, file: File) {
        if (!file.exists()) {
            Log.e("ClipboardUtils", "File not found: ${file.absolutePath}")
            return
        }
        val uri = FileProvider.getUriForFile(context, "com.example.superutils.fileprovider", file)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newUri(context.contentResolver, null, uri)
        clipboard.setPrimaryClip(clip)
    }
}

object ShareUtils {
    fun shareFile(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(context, "com.example.superutils.fileprovider", file)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share via"))
        } catch (e: Exception) {
            Log.d("ShareUtils", e.message.orEmpty())
            Toast.makeText(context, "Failed to share file.", Toast.LENGTH_SHORT).show()
        }
    }
}

object DownloadUtils {
    @SuppressLint("NewApi")
    fun copyFileToDownloads(context: Context, sourceFile: File) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, sourceFile.name)
            put(MediaStore.Downloads.MIME_TYPE, MimeHelper.getMimeType(sourceFile.name))
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            resolver.openOutputStream(it)?.use { outputStream ->
                FileInputStream(sourceFile).use { inputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
    }

    @SuppressLint("NewApi")
    fun launchDefaultFileManager(context: Context) {
        launchAppWithFallback(
            context,
            "com.google.android.apps.nbu.files",
            "com.google.android.apps.nbu.files.home.HomeActivity"
        )
    }

    fun launchAppWithFallback(context: Context, packageName: String, fallbackActivity: String? = null) {
        val pm = context.packageManager
        val launchIntent = pm.getLaunchIntentForPackage(packageName)

        try {
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            } else if (fallbackActivity != null) {
                val intent = Intent().apply {
                    component = ComponentName(packageName, fallbackActivity)
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } else {
                // Open Play Store fallback
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch $packageName", Toast.LENGTH_SHORT).show()
        }
    }

}

