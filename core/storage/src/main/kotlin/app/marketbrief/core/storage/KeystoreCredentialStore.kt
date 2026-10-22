package app.marketbrief.core.storage

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import app.marketbrief.core.data.positioning.CredentialStore
import app.marketbrief.core.data.positioning.MyfxbookCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Keeps the Myfxbook login encrypted with an AES-GCM key that never leaves the Android Keystore.
 * Excluded from cloud backup by the app's backup rules; clearing the app's data removes it.
 */
class KeystoreCredentialStore(context: Context) : CredentialStore {

    private val prefs = context.getSharedPreferences("myfxbook_credentials", Context.MODE_PRIVATE)

    override suspend fun get(): MyfxbookCredentials? = withContext(Dispatchers.IO) {
        val email = prefs.getString(KEY_EMAIL, null) ?: return@withContext null
        val blob = prefs.getString(KEY_PASSWORD, null) ?: return@withContext null
        try {
            MyfxbookCredentials(email, decrypt(blob))
        } catch (_: Exception) {
            // Key was invalidated (for example after a device security change). Treat as signed out.
            prefs.edit().clear().apply()
            null
        }
    }

    override suspend fun save(credentials: MyfxbookCredentials) = withContext(Dispatchers.IO) {
        prefs.edit()
            .putString(KEY_EMAIL, credentials.email)
            .putString(KEY_PASSWORD, encrypt(credentials.password))
            .apply()
    }

    override suspend fun clear() = withContext(Dispatchers.IO) {
        prefs.edit().clear().apply()
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (ks.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val out = cipher.iv + cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(out, Base64.NO_WRAP)
    }

    private fun decrypt(blob: String): String {
        val bytes = Base64.decode(blob, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORM).apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv)) }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val ALIAS = "marketbrief_myfxbook_key"
        const val TRANSFORM = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val KEY_EMAIL = "email"
        const val KEY_PASSWORD = "password"
    }
}
