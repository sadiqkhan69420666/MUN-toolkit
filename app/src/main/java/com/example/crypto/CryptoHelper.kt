package com.example.crypto

import android.util.Base64
import java.security.*
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoHelper {
    private const val RSA_ALGORITHM = "RSA/ECB/PKCS1Padding"
    private const val AES_GCM_ALGORITHM = "AES/GCM/NoPadding"
    private const val SIGNATURE_ALGORITHM = "SHA256withRSA"

    fun generateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048)
        return kpg.generateKeyPair()
    }

    fun sign(data: String, privateKey: PrivateKey): String {
        val signer = Signature.getInstance(SIGNATURE_ALGORITHM)
        signer.initSign(privateKey)
        signer.update(data.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(signer.sign(), Base64.NO_WRAP)
    }

    fun verify(data: String, signatureBase64: String, publicKey: PublicKey): Boolean {
        return try {
            val verifier = Signature.getInstance(SIGNATURE_ALGORITHM)
            verifier.initVerify(publicKey)
            verifier.update(data.toByteArray(Charsets.UTF_8))
            val sigBytes = Base64.decode(signatureBase64, Base64.NO_WRAP)
            verifier.verify(sigBytes)
        } catch (e: Exception) {
            false
        }
    }

    fun encryptPrivateMessage(
        message: String,
        recipientPublicKey: PublicKey
    ): EncryptedPayload {
        // 1. Generate local AES key
        val keyGen = KeyGenerator.getInstance("AES")
        keyGen.init(256)
        val aesKey = keyGen.generateKey()

        // 2. Encrypt AES key using recipient's RSA Public Key
        val rsaCipher = Cipher.getInstance(RSA_ALGORITHM)
        rsaCipher.init(Cipher.ENCRYPT_MODE, recipientPublicKey)
        val encryptedAesKeyBytes = rsaCipher.doFinal(aesKey.encoded)
        val encryptedAesKeyBase64 = Base64.encodeToString(encryptedAesKeyBytes, Base64.NO_WRAP)

        // 3. Encrypt message body using AES-GCM
        val aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(iv)
        val parameterSpec = GCMParameterSpec(128, iv)
        aesCipher.init(Cipher.ENCRYPT_MODE, aesKey, parameterSpec)
        val ciphertext = aesCipher.doFinal(message.toByteArray(Charsets.UTF_8))

        val encryptedContentBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP)
        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)

        return EncryptedPayload(
            encryptedAesKey = encryptedAesKeyBase64,
            encryptedContent = encryptedContentBase64,
            iv = ivBase64
        )
    }

    fun decryptPrivateMessage(
        payload: EncryptedPayload,
        myPrivateKey: PrivateKey
    ): String {
        // 1. Decrypt AES key
        val rsaCipher = Cipher.getInstance(RSA_ALGORITHM)
        rsaCipher.init(Cipher.DECRYPT_MODE, myPrivateKey)
        val encryptedAesKeyBytes = Base64.decode(payload.encryptedAesKey, Base64.NO_WRAP)
        val aesKeyBytes = rsaCipher.doFinal(encryptedAesKeyBytes)
        val aesKey = SecretKeySpec(aesKeyBytes, "AES")

        // 2. Decrypt message body with AES
        val aesCipher = Cipher.getInstance(AES_GCM_ALGORITHM)
        val iv = Base64.decode(payload.iv, Base64.NO_WRAP)
        val parameterSpec = GCMParameterSpec(128, iv)
        aesCipher.init(Cipher.DECRYPT_MODE, aesKey, parameterSpec)
        val encryptedBytes = Base64.decode(payload.encryptedContent, Base64.NO_WRAP)
        val decryptedBytes = aesCipher.doFinal(encryptedBytes)

        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun publicKeyFromBase64(base64: String): PublicKey {
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = X509EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePublic(spec)
    }

    fun privateKeyFromBase64(base64: String): PrivateKey {
        val keyBytes = Base64.decode(base64, Base64.NO_WRAP)
        val spec = java.security.spec.PKCS8EncodedKeySpec(keyBytes)
        val kf = KeyFactory.getInstance("RSA")
        return kf.generatePrivate(spec)
    }

    fun keyToBase64(key: Key): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }
}

data class EncryptedPayload(
    val encryptedAesKey: String,
    val encryptedContent: String,
    val iv: String
)
