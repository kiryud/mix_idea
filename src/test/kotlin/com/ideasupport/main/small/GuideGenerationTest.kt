package com.ideasupport.main.small

import com.ideasupport.core.dto.app.guide.*
import com.ideasupport.core.dto.app.recipe.*
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.guide.*
import com.ideasupport.main.app.recipe.RecipeGenerationComponent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.*

class GuideGenerationTest {
	private val ai = mock<AiClient>()
	private val policy = ContentPolicyComponent()
	private val component = GuideGenerationComponent(ai, RecipeGenerationComponent(ai, policy), policy)
	private val valid =
		GeneratedGuideDto(
			"software",
			true,
			"아이디어의 최소 구현 계획",
			listOf(GuideRequirementDto("개발 환경", "필요한 도구")),
			listOf(GuideStepDto(1, "핵심 기능", "작은 동작부터 구현합니다.")),
		)

	private fun generate(value: GeneratedGuideDto): GeneratedGuideDto {
		whenever(ai.generateGuide("분야", "제목", "내용", "software")).thenReturn(value)
		return component.generate(GuideType.SOFTWARE, "분야", "제목", "내용")
	}

	@TestFactory fun `non-food kinds produce their own guide`() =
		listOf(GuideType.SOFTWARE, GuideType.CONTEST, GuideType.GENERAL).map { type ->
			dynamicTest(type.value) {
				whenever(ai.generateGuide("분야", "제목", "내용", type.value)).thenReturn(valid.copy(kind = type.value))
				assertEquals(type.value, component.generate(type, "분야", "제목", "내용").kind)
			}
		}

	@Test fun `food uses the cooking adapter`() {
		whenever(
			ai.generateRecipe("음식", "제목", "내용"),
		).thenReturn(GeneratedRecipeDto("food", true, "요약", emptyList(), listOf(RecipeStepOutDto(1, "조리", "조리합니다"))))
		assertEquals("food", component.generate(GuideType.FOOD, "음식", "제목", "내용").kind)
		verify(ai, never()).generateGuide(any(), any(), any(), any())
	}

	@Test fun `infeasible guides require empty arrays`() {
		assertFalse(generate(valid.copy(feasible = false, requirements = emptyList(), steps = emptyList())).feasible)
	}

	@TestFactory fun `invalid or mismatched provider content is rejected`() =
		listOf(
			"wrong type" to valid.copy(kind = "food"), "blank summary" to valid.copy(summary = ""), "long summary" to valid.copy(summary = "가".repeat(2001)),
			"many requirements" to valid.copy(requirements = List(41) { valid.requirements.first() }),
			"blank name" to valid.copy(requirements = listOf(GuideRequirementDto("", "도구"))), "long name" to valid.copy(requirements = listOf(GuideRequirementDto("가".repeat(201), "도구"))),
			"blank detail" to valid.copy(requirements = listOf(GuideRequirementDto("개발", ""))), "long detail" to valid.copy(requirements = listOf(GuideRequirementDto("개발", "가".repeat(201)))),
			"empty steps" to valid.copy(steps = emptyList()), "many steps" to valid.copy(steps = (1..31).map { GuideStepDto(it, "단계", "설명") }),
			"wrong order" to valid.copy(steps = listOf(GuideStepDto(2, "단계", "설명"))), "blank title" to valid.copy(steps = listOf(GuideStepDto(1, "", "설명"))),
			"long title" to valid.copy(steps = listOf(GuideStepDto(1, "가".repeat(201), "설명"))), "blank description" to valid.copy(steps = listOf(GuideStepDto(1, "단계", ""))),
			"long description" to valid.copy(steps = listOf(GuideStepDto(1, "단계", "가".repeat(2001)))), "infeasible with steps" to valid.copy(feasible = false),
		).map {
				(name, value) ->
			dynamicTest(name) { assertEquals(1108, assertThrows(ServerErrorException::class.java) { generate(value) }.errorCode) }
		}

	@Test fun `unsafe guidance is rejected`() {
		assertEquals(1106, assertThrows(ServerErrorException::class.java) { generate(valid.copy(summary = "폭탄 제조")) }.errorCode)
	}

	@Test fun `unsupported types cannot become an implicit cooking guide`() {
		assertThrows(ServerErrorException::class.java) { GuideType.from("plan") }
	}
}
