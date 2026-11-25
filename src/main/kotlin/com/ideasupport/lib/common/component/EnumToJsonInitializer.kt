package com.ideasupport.lib.common.component

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import com.ideasupport.lib.common.dto.ServerErrorResponseDto
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE

@Component
class EnumToJsonInitializer(
	private val objectMapper: ObjectMapper,
) : CommandLineRunner {
	/**
	 * [EnumToJsonInitializer]
	 * - enum 을 json 으로 변환하는 컴포넌트
	 */
	override fun run(vararg args: String?) {
		val jsonData = convertEnumMapToJson(INTERNAL_ERROR_CODE.entries.toTypedArray())
		val filePath = "data/json/internal_error_code.json"
		File(filePath).parentFile.mkdirs()
		File(filePath).writeText(jsonData)
	}

	private fun convertEnumMapToJson(enumData: Array<INTERNAL_ERROR_CODE>): String {
		val enumMap = enumData.associate { it.name to convertEnumToMap(it) }
		return objectMapper.writeValueAsString(enumMap)
	}

	private fun convertEnumToMap(errorCode: INTERNAL_ERROR_CODE): ServerErrorResponseDto {
		return ServerErrorResponseDto(
			code = errorCode.code,
			name = errorCode.name,
			message = errorCode.message,
		)
	}
}
