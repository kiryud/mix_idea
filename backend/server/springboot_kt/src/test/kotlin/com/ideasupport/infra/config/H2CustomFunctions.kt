package com.ideasupport.infra.config

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

object H2CustomFunctions {
	private val objectMapper = ObjectMapper()

	@JvmStatic
	fun jsonExtract(
		json: String,
		path: String,
	): String? {
		// JSON Path를 JSON Pointer로 변환
		val jsonPointer = path.replace("$", "").replace(".", "/")
		val rootNode: JsonNode = objectMapper.readTree(json)
		val resultNode: JsonNode? = rootNode.at(jsonPointer)
		return resultNode?.toString()
	}
}
