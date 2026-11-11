package com.ideasupport.main.app.recipe
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ideasupport.infra.repository.RecipeRepository
import com.ideasupport.core.entity.RecipeEntity
import com.ideasupport.core.dto.app.recipe.*

@Component
class RecipeComponent(private val recipes: RecipeRepository, private val mapper: ObjectMapper) {
	fun find(
		id: String,
		kind: String = "food",
	): RecipeEntity? = recipes.findById(id).orElse(null)?.takeIf { it.kind == kind }

	fun output(row: RecipeEntity) =
		RecipeOutDto(
			row.ideaId,
			row.kind,
			row.feasible,
			row.summary,
			mapper.readValue(row.ingredients),
			mapper.readValue(row.steps),
			row.source,
		)

	@Transactional fun save(
		id: String,
		result: GeneratedRecipeDto,
		source: String,
	): RecipeEntity =
		recipes.saveAndFlush(
			RecipeEntity(
				id,
				result.kind,
				result.feasible,
				result.summary,
				mapper.writeValueAsString(result.ingredients),
				mapper.writeValueAsString(result.steps),
				source,
			),
		)
}
