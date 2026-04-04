package io.github.sj42tech.route42.data

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.ByteArrayOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal interface ProfilesStorageCodec {
    fun decode(bytes: ByteArray): ByteArray

    fun encode(bytes: ByteArray): ByteArray
}

internal object EncryptedProfilesCodec : ProfilesStorageCodec {
    private const val KeyAlias = "route42_profiles_snapshot_v1"
    private const val KeyStoreName = "AndroidKeyStore"
    private const val Transformation = "AES/GCM/NoPadding"
    private const val IvSizeBytes = 12
    private const val AuthTagSizeBits = 128
    private val Header = byteArrayOf('R'.code.toByte(), '4'.code.toByte(), '2'.code.toByte(), 'E'.code.toByte(), '1'.code.toByte())

    override fun decode(bytes: ByteArray): ByteArray {
        if (!isEncrypted(bytes)) {
            return bytes
        }

        require(bytes.size > Header.size + IvSizeBytes) { "Encrypted profile snapshot is truncated" }

        val iv = bytes.copyOfRange(Header.size, Header.size + IvSizeBytes)
        val payload = bytes.copyOfRange(Header.size + IvSizeBytes, bytes.size)
        val cipher = Cipher.getInstance(Transformation).apply {
            init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(AuthTagSizeBits, iv))
        }
        return cipher.doFinal(payload)
    }

    override fun encode(bytes: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(Transformation).apply {
            init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        }
        val iv = cipher.iv
        require(iv.size == IvSizeBytes) { "Unexpected GCM IV size" }

        val payload = cipher.doFinal(bytes)
        return ByteArrayOutputStream(Header.size + iv.size + payload.size).use { output ->
            output.write(Header)
            output.write(iv)
            output.write(payload)
            output.toByteArray()
        }
    }

    private fun isEncrypted(bytes: ByteArray): Boolean {
        if (bytes.size < Header.size) {
            return false
        }
        return bytes.copyOfRange(0, Header.size).contentEquals(Header)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KeyStoreName).apply { load(null) }
        (keyStore.getKey(KeyAlias, null) as? SecretKey)?.let { return it }

        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KeyStoreName).run {
            init(
                KeyGenParameterSpec.Builder(
                    KeyAlias,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .setKeySize(256)
                    .build(),
            )
            generateKey()
        }
    }
}
