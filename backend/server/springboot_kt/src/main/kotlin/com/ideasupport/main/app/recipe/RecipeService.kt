package com.ideasupport.main.app.recipe
import org.springframework.stereotype.Service
import com.ideasupport.core.dto.app.recipe.CreateRecipeOutDto
import com.ideasupport.core.dto.app.recipe.RecipeOutDto
import com.ideasupport.core.dto.app.recipe.IngredientOutDto
import com.ideasupport.core.dto.app.recipe.RecipeStepOutDto
import com.ideasupport.main.app.guide.GuideService

@Service
class RecipeService(private val guides: GuideService) {
	fun create(id: String): CreateRecipeOutDto {
		val result = guides.create(id)
		val guide = result.guide
		return CreateRecipeOutDto(
			RecipeOutDto(
				guide.ideaId,
				guide.kind,
				guide.feasible,
				guide.summary,
				guide.requirements.map {
					IngredientOutDto(it.name, it.detail)
				},
				guide.steps.map { RecipeStepOutDto(it.order, it.title, it.description) },
				guide.source,
			),
			result.cached,
		)
	}
}
