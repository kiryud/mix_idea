package com.ideasupport.lib.common.component

import java.util.*
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CryptionComponent {
	@Value("\${aes.key}")
	private lateinit var key: String

	fun encrypt(text: String): String {
		return kotlin.runCatching {
			getAesKey(text)
		}.getOrElse {
			throw RuntimeException(it)
		}
	}

	fun decrypt(clientKey: String): String {
		return kotlin.runCatching {
			val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
			val keySpec = SecretKeySpec(key.toByteArray(), "AES")
			val ivParameterSpec = IvParameterSpec(key.substring(0, 16).toByteArray())
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivParameterSpec)

			val decoedBytes = Base64.getDecoder().decode(clientKey.toByteArray())
			val decrypted = cipher.doFinal(decoedBytes)
			String(decrypted).trim { it <= ' ' }
		}.getOrElse {
			throw RuntimeException(it)
		}
	}

	private fun getAesKey(text: String): String {
		val cipher: Cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
		val keySpec = SecretKeySpec(key.toByteArray(), "AES")
		val ivParameterSpec = IvParameterSpec(key.substring(0, 16).toByteArray())
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivParameterSpec)

		val encodedBytes: ByteArray = cipher.doFinal(text.toByteArray())
		val encrypted: ByteArray = Base64.getEncoder().encode(encodedBytes)
		return String(encrypted).trim { it <= ' ' }
	}
}
