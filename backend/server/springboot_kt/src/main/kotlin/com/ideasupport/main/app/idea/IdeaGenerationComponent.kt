package com.ideasupport.main.app.idea
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.ObjectMapper
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.core.dto.app.idea.GeneratedIdeaDto
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class IdeaGenerationComponent(private val ai: AiClient, private val policy: ContentPolicyComponent, private val mapper: ObjectMapper) {
	val source get() = ai.source

	fun key(
		categoryId: String,
		words: List<String>,
		direction: String,
	): String = policy.hash(mapper.writeValueAsString(listOf(categoryId, words.sorted(), direction, ai.cacheVersion)))

	fun generate(
		category: String,
		words: List<String>,
		direction: String,
		variation: String?,
	): GeneratedIdeaDto {
		policy.check(direction)
		val result = ai.generateIdea(category, words, direction, variation)
		if (result.title.isBlank() || result.title.length > 160 || result.content.isBlank() || result.content.length > 10000) {
			throw ServerErrorException(
				INTERNAL_ERROR_CODE.AI_RESPONSE_INVALID,
			)
		}
		policy.check(result.title + " " + result.content)
		return result
	}
}
