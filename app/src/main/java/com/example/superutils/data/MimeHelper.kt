package com.example.superutils.data

object MimeHelper {
    //use as filePath in getmimetype
    val TXT: String = ".txt"
    val HTML: String = ".html"
    val HTM: String = ".htm"
    val JPG: String = ".jpg"
    val JPEG: String = ".jpeg"
    val PNG: String = ".png"
    val GIF: String = ".gif"
    val BMP: String = ".bmp"
    val PDF: String = ".pdf"
    val DOC: String = ".doc"
    val DOCX: String = ".docx"
    val XLS: String = ".xls"
    val XLSX: String = ".xlsx"
    val ZIP: String = ".zip"
    val RAR: String = ".rar"
    val MP3: String = ".mp3"
    val MP4: String = ".mp4"

    private val mimeTypes = mapOf(
        ".txt" to "text/plain",
        ".html" to "text/html",
        ".htm" to "text/html",
        ".jpg" to "image/jpeg",
        ".jpeg" to "image/jpeg",
        ".png" to "image/png",
        ".gif" to "image/gif",
        ".bmp" to "image/bmp",
        ".pdf" to "application/pdf",
        ".doc" to "application/msword",
        ".docx" to "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        ".xls" to "application/vnd.ms-excel",
        ".xlsx" to "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        ".zip" to "application/zip",
        ".rar" to "application/vnd.rar",
        ".mp3" to "audio/mpeg",
        ".mp4" to "video/mp4"
        // Add more as needed
    )

    fun getMimeType(filePath: String): String {
        val ext = filePath.substringAfterLast('.', "").lowercase().let { ".$it" }
        return mimeTypes[ext] ?: "application/octet-stream"
    }

    fun getExtension(mimeType: String): String {
        return mimeTypes.entries.firstOrNull { it.value == mimeType }?.key!!
    }
}