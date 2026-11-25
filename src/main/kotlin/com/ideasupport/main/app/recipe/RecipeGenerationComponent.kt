package com.ideasupport.main.app.recipe
import org.springframework.stereotype.Component
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.core.dto.app.recipe.*
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class RecipeGenerationComponent(private val ai: AiClient, private val policy: ContentPolicyComponent) {
	val source get() = ai.source

	fun generate(
		category: String,
		title: String,
		content: String,
	): GeneratedRecipeDto {
		val result = ai.generateRecipe(category, title, content)
		val valid =
			result.kind == "food" && result.summary.isNotBlank() && result.summary.length <= 2000 && result.ingredients.size <= 40 && result.steps.size <= 30 &&
				result.ingredients.all { it.name.isNotBlank() && it.name.length <= 200 && it.amount.isNotBlank() && it.amount.length <= 200 } &&
				result.steps.withIndex().all {
						(index, step) ->
					step.order == index + 1 && step.title.isNotBlank() && step.title.length <= 200 && step.description.isNotBlank() && step.description.length <= 2000
				} &&
				(if (result.feasible) result.steps.isNotEmpty() else result.ingredients.isEmpty() && result.steps.isEmpty())
		if (!valid) throw ServerErrorException(INTERNAL_ERROR_CODE.AI_RESPONSE_INVALID)
		val text =
			buildList {
				add(result.summary)
				result.ingredients.forEach {
					add(it.name)
					add(it.amount)
				}
				result.steps.forEach {
					add(it.title)
					add(it.description)
				}
			}.joinToString(" ")
		policy.check(text)
		return result
	}
}
