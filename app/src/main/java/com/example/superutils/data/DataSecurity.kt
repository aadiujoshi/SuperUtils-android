package com.example.superutils.data

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object DataSecurity {
    // 16-byte key and IV (128-bit AES)
    //ENCRYPTION KEY
    private const val keyString = "1234567890abcdef" // must be 16 bytes
    //ENCRYPTION SEED
    private const val ivString = "abcdef1234567890"  // must be 16 bytes

    private val keySpec = SecretKeySpec(keyString.toByteArray(), "AES")
    private val ivSpec = IvParameterSpec(ivString.toByteArray())

    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.DEFAULT).trim()
    }

    fun decrypt(encryptedBase64: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        val decoded = Base64.decode(encryptedBase64, Base64.DEFAULT)
        val decrypted = cipher.doFinal(decoded)
        return String(decrypted, Charsets.UTF_8)
    }
}