package com.ideasupport.main.app.guide

import com.ideasupport.core.dto.app.guide.*
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.recipe.RecipeGenerationComponent
import org.springframework.stereotype.Component

@Component
class GuideGenerationComponent(
	private val ai: AiClient,
	private val cooking: RecipeGenerationComponent,
	private val policy: ContentPolicyComponent,
) {
	val source get() = ai.source

	fun generate(
		type: GuideType,
		category: String,
		title: String,
		content: String,
	): GeneratedGuideDto {
		val result =
			if (type == GuideType.FOOD) {
				val recipe = cooking.generate(category, title, content)
				GeneratedGuideDto(
					"food",
					recipe.feasible,
					recipe.summary,
					recipe.ingredients.map {
						GuideRequirementDto(it.name, it.amount)
					},
					recipe.steps.map { GuideStepDto(it.order, it.title, it.description) },
				)
			} else {
				ai.generateGuide(category, title, content, type.value)
			}
		val valid =
			result.kind == type.value && result.summary.isNotBlank() && result.summary.length <= 2000 && result.requirements.size <= 40 && result.steps.size <= 30 &&
				result.requirements.all { it.name.isNotBlank() && it.name.length <= 200 && it.detail.isNotBlank() && it.detail.length <= 200 } &&
				result.steps.withIndex().all {
						(index, step) ->
					step.order == index + 1 && step.title.isNotBlank() && step.title.length <= 200 && step.description.isNotBlank() && step.description.length <= 2000
				} &&
				(if (result.feasible) result.steps.isNotEmpty() else result.requirements.isEmpty() && result.steps.isEmpty())
		if (!valid) throw ServerErrorException(INTERNAL_ERROR_CODE.AI_RESPONSE_INVALID)
		policy.check(
			(
				listOf(result.summary) +
					result.requirements.flatMap {
						listOf(it.name, it.detail)
					} + result.steps.flatMap { listOf(it.title, it.description) }
			).joinToString(" "),
		)
		return result
	}
}
