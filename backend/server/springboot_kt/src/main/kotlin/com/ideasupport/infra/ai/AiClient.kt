package com.ideasupport.infra.ai
import com.ideasupport.core.dto.app.idea.GeneratedIdeaDto
import com.ideasupport.core.dto.app.recipe.GeneratedRecipeDto
import com.ideasupport.core.dto.app.word.WordOutDto
import com.ideasupport.core.dto.app.guide.GeneratedGuideDto

interface AiClient {
	val source: String
	val cacheVersion: String

	fun generateIdea(
		category: String,
		words: List<String>,
		direction: String,
		variation: String?,
	): GeneratedIdeaDto

	fun generateRecipe(
		category: String,
		title: String,
		content: String,
	): GeneratedRecipeDto

	fun generateGuide(
		category: String,
		title: String,
		content: String,
		kind: String,
	): GeneratedGuideDto

	fun selectWords(
		input: String,
		candidates: List<WordOutDto>,
	): List<String>
}
