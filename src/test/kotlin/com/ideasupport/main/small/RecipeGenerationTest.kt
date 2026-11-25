package com.ideasupport.main.small

import com.ideasupport.core.dto.app.recipe.*
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.recipe.RecipeGenerationComponent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.Mockito.*

class RecipeGenerationTest {
	private val valid =
		GeneratedRecipeDto("food", true, "안전한 만드는 법", listOf(IngredientOutDto("감자", "2개")), listOf(RecipeStepOutDto(1, "준비", "재료를 씻습니다")))

	private fun generate(result: GeneratedRecipeDto): GeneratedRecipeDto {
		val ai = mock(AiClient::class.java)
		`when`(ai.generateRecipe("분야", "제목", "내용")).thenReturn(result)
		return RecipeGenerationComponent(ai, ContentPolicyComponent()).generate("분야", "제목", "내용")
	}

	@Test fun `valid food recipe is returned`() {
		assertEquals(valid, generate(valid))
	}

	@Test fun `non-food execution plan is rejected as invalid provider output`() {
		assertEquals(1108, assertThrows(ServerErrorException::class.java) { generate(valid.copy(kind = "plan")) }.errorCode)
	}

	@Test fun `infeasible result requires explanation and empty arrays`() {
		assertFalse(generate(valid.copy(feasible = false, ingredients = emptyList(), steps = emptyList())).feasible)
	}

	@TestFactory fun `malformed provider output is rejected`() =
		listOf(
			"unknown kind" to valid.copy(kind = "other"),
			"blank summary" to valid.copy(summary = " "),
			"long summary" to valid.copy(summary = "가".repeat(2001)),
			"too many ingredients" to valid.copy(ingredients = List(41) { valid.ingredients.first() }),
			"blank ingredient" to valid.copy(ingredients = listOf(IngredientOutDto("", "1개"))),
			"blank amount" to valid.copy(ingredients = listOf(IngredientOutDto("감자", ""))),
			"long amount" to valid.copy(ingredients = listOf(IngredientOutDto("감자", "가".repeat(201)))),
			"empty feasible steps" to valid.copy(steps = emptyList()),
			"wrong step order" to valid.copy(steps = listOf(RecipeStepOutDto(2, "준비", "설명"))),
			"blank step title" to valid.copy(steps = listOf(RecipeStepOutDto(1, "", "설명"))),
			"long description" to valid.copy(steps = listOf(RecipeStepOutDto(1, "준비", "가".repeat(2001)))),
			"too many steps" to valid.copy(steps = (1..31).map { RecipeStepOutDto(it, "단계", "설명") }),
			"infeasible with steps" to valid.copy(feasible = false),
		).map {
				(name, result) ->
			dynamicTest(name) { assertEquals(1108, assertThrows(ServerErrorException::class.java) { generate(result) }.errorCode) }
		}

	@TestFactory fun `all provider text fields apply the content policy`() =
		listOf(
			"summary" to valid.copy(summary = "폭탄 제조"),
			"ingredient name" to valid.copy(ingredients = listOf(IngredientOutDto("폭탄 제조", "1개"))),
			"ingredient amount" to valid.copy(ingredients = listOf(IngredientOutDto("감자", "폭탄 제조"))),
			"step title" to valid.copy(steps = listOf(RecipeStepOutDto(1, "폭탄 제조", "설명"))),
			"step description" to valid.copy(steps = listOf(RecipeStepOutDto(1, "준비", "폭탄 제조"))),
		).map {
				(name, result) ->
			dynamicTest(name) { assertEquals(1106, assertThrows(ServerErrorException::class.java) { generate(result) }.errorCode) }
		}
}
