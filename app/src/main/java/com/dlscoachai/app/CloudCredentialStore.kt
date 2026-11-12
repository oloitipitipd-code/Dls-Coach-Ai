package com.dlscoachai.app

import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts the optional cloud gateway token at rest with an Android Keystore AES key.
 * The key is non-exportable and remains on this device. If ciphertext cannot be
 * decrypted (for example after key invalidation), the stored value is discarded.
 */
object CloudCredentialStore {
    private const val KEY_ALIAS = "dls_coach_cloud_token_v1"
    private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
    private const val PREF_CIPHERTEXT = "cloud_ai_access_token_enc"

    fun read(preferences: SharedPreferences): String {
        val encoded = preferences.getString(PREF_CIPHERTEXT, null) ?: return ""
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            require(payload.size > 12) { "Encrypted token payload is invalid." }
            val buffer = ByteBuffer.wrap(payload)
            val iv = ByteArray(12).also(buffer::get)
            val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrElse {
            preferences.edit().remove(PREF_CIPHERTEXT).apply()
            ""
        }
    }

    fun write(preferences: SharedPreferences, token: String) {
        if (token.isBlank()) {
            preferences.edit().remove(PREF_CIPHERTEXT).apply()
            return
        }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(token.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(cipher.iv.size + encrypted.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        preferences.edit()
            .putString(PREF_CIPHERTEXT, Base64.encodeToString(payload, Base64.NO_WRAP))
            .remove("cloud_ai_access_token")
            .apply()
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
