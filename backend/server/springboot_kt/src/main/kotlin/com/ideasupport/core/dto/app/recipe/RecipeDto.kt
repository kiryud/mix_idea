package com.ideasupport.core.dto.app.recipe

import io.swagger.v3.oas.annotations.media.Schema

data class IngredientOutDto(val name: String, val amount: String)

data class RecipeStepOutDto(val order: Int, val title: String, val description: String)

data class GeneratedRecipeDto(
	@field:Schema(allowableValues = ["food"]) val kind: String,
	val feasible: Boolean,
	val summary: String,
	val ingredients: List<IngredientOutDto>,
	val steps: List<RecipeStepOutDto>,
)

data class RecipeOutDto(
	val ideaId: String,
	@field:Schema(allowableValues = ["food", "software", "contest", "general"]) val kind: String,
	val feasible: Boolean,
	val summary: String,
	val ingredients: List<IngredientOutDto>,
	val steps: List<RecipeStepOutDto>,
	val source: String,
)

data class CreateRecipeOutDto(val recipe: RecipeOutDto, val cached: Boolean)
