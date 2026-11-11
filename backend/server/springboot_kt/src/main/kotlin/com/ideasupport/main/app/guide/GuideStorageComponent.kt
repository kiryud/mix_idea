package com.ideasupport.main.app.guide

import com.ideasupport.core.dto.app.guide.*
import com.ideasupport.core.dto.app.recipe.*
import com.ideasupport.core.entity.RecipeEntity
import com.ideasupport.main.app.recipe.RecipeComponent
import org.springframework.stereotype.Component

/** The existing table remains compatible with saved food recipes and old clients. */
@Component
class GuideStorageComponent(private val recipes: RecipeComponent) {
	fun find(
		id: String,
		type: GuideType,
	): RecipeEntity? = recipes.find(id, type.value)

	fun output(row: RecipeEntity): GuideOutDto {
		val value = recipes.output(row)
		val type = GuideType.from(value.kind)
		return GuideOutDto(
			value.ideaId, type.value, type.label, type.requirementsLabel, value.feasible, value.summary,
			value.ingredients.map {
				GuideRequirementDto(it.name, it.amount)
			},
			value.steps.map { GuideStepDto(it.order, it.title, it.description) }, value.source,
		)
	}

	fun save(
		id: String,
		result: GeneratedGuideDto,
		source: String,
	): RecipeEntity =
		recipes.save(
			id,
			GeneratedRecipeDto(
				result.kind,
				result.feasible,
				result.summary,
				result.requirements.map {
					IngredientOutDto(it.name, it.detail)
				},
				result.steps.map { RecipeStepOutDto(it.order, it.title, it.description) },
			),
			source,
		)
}
