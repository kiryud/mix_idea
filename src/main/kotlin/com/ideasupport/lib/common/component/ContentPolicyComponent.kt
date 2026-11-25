package com.ideasupport.lib.common.component
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.text.Normalizer
import java.util.Locale
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class ContentPolicyComponent {
	fun normalize(value: String): String = Normalizer.normalize(value, Normalizer.Form.NFKC).trim().replace(Regex("\\s+"), " ")

	fun key(value: String): String = normalize(value).lowercase(Locale.ROOT).replace(" ", "")

	fun hash(value: String): String =
		MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") {
			"%02x".format(it)
		}

	fun check(value: String) {
		if (listOf(
				"아동 성착취",
				"폭탄 제조",
				"자살 방법",
			).any { key(value).contains(key(it)) }
		) {
			throw ServerErrorException(INTERNAL_ERROR_CODE.UNSAFE_CONTENT)
		}
	}

	fun requireInput(condition: Boolean) {
		if (!condition) throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_INPUT)
	}
}
