package com.ideasupport.infra.ai
import org.springframework.stereotype.Component
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import com.ideasupport.core.dto.app.idea.GeneratedIdeaDto
import com.ideasupport.core.dto.app.recipe.*
import com.ideasupport.core.dto.app.word.WordOutDto
import com.ideasupport.core.dto.app.guide.*

@Component
@ConditionalOnProperty(name = ["app.ai.mode"], havingValue = "mock", matchIfMissing = true)
class MockAiClient : AiClient {
	override val source = "mock"
	override val cacheVersion = "mock-v1"

	override fun generateIdea(
		category: String,
		words: List<String>,
		direction: String,
		variation: String?,
	): GeneratedIdeaDto {
		val suffix = variation?.let { " · 변형 ${it.take(6)}" } ?: ""
		return GeneratedIdeaDto(
			"${words.joinToString(" × ")}$suffix",
			"[시연 예시] $category 분야에서 ${words.joinToString(
				", ",
			)}를 결합하는 아이디어입니다. ${if (direction.isBlank()) "작은 시제품으로 가능성을 확인해 보세요." else "'$direction' 방향으로 구성하고 작은 시제품으로 확인해 보세요."}",
		)
	}

	override fun generateRecipe(
		category: String,
		title: String,
		content: String,
	): GeneratedRecipeDto =
		GeneratedRecipeDto(
			kind = "food",
			feasible = true,
			summary = "[시연 예시] $title 실행 순서입니다. 실제 실행 전 재료와 조건을 확인해 주세요.",
			ingredients = listOf(IngredientOutDto("아이디어에 필요한 재료 또는 도구", "소규모 시제품 분량")),
			steps = listOf(RecipeStepOutDto(1, "준비", "필요한 재료와 도구를 목록으로 정리합니다."), RecipeStepOutDto(2, "시제품", "안전한 범위에서 작은 시제품을 만들고 결과를 확인합니다.")),
		)

	// 시연 결과도 선택한 분야와 후속 가이드 유형을 보존한다.
	override fun generateGuide(
		category: String,
		title: String,
		content: String,
		kind: String,
	): GeneratedGuideDto {
		val sections =
			when (kind) {
				"software" ->
					listOf(
						"핵심 사용자와 MVP" to "$title 사용자가 가장 먼저 수행할 행동과 필수 기능 3개를 정합니다.",
						"화면과 데이터 설계" to "입력 화면, 결과 화면과 저장할 데이터를 정의하고 연결 흐름을 정리합니다.",
						"최소 기능 구현" to "가장 작은 동작부터 구현하고 화면과 서버를 연결합니다.",
						"기능 검증" to "정상 입력, 잘못된 입력과 네트워크 실패를 테스트합니다.",
						"사용자 피드백" to "작은 사용자 그룹에 시연하고 개선 우선순위를 정합니다.",
					)
				"contest" ->
					listOf(
						"문제와 대상" to "$title 이 해결할 문제와 영향을 받는 대상을 구체화합니다.",
						"해결 방식과 차별점" to "기존 대안과 비교해 아이디어의 핵심 차별점을 설명합니다.",
						"시제품과 실험" to "가설을 확인할 작은 시제품과 측정 방법을 설계합니다.",
						"효과와 실현 가능성" to "예상 효과, 비용과 기술·운영 위험을 점검합니다.",
						"제안서 구성" to "문제, 해결안, 검증 결과, 기대 효과 순서로 기획안을 구성합니다.",
					)
				else ->
					listOf(
						"목표 구체화" to "$category 분야에서 $title 의 사용자와 달성할 목표를 정합니다.",
						"작은 실험" to "가장 중요한 가설 하나를 적은 비용으로 확인할 방법을 정합니다.",
						"실행 순서" to "준비, 첫 시도, 피드백 반영 순서로 행동을 나눕니다.",
						"성공 기준" to "관찰 가능한 결과와 측정 시점을 정합니다.",
						"개선과 다음 단계" to "실험 결과를 기록하고 유지·변경·중단 여부를 판단합니다.",
					)
			}
		return GeneratedGuideDto(
			kind,
			true,
			"[시연 예시] $title 에 맞춘 ${if (kind == "software") {
				"구현 가이드"
			} else if (kind == "contest") {
				"공모전 기획안"
			} else {
				"실행 계획"
			}}입니다.",
			listOf(GuideRequirementDto("아이디어 요약", title.take(200)), GuideRequirementDto("검증 자료", "사용자 요구와 작은 실험 결과")),
			sections.mapIndexed {
					index,
					(heading, detail),
				->
				GuideStepDto(index + 1, heading, detail)
			},
		)
	}

	override fun selectWords(
		input: String,
		candidates: List<WordOutDto>,
	): List<String> = emptyList()
}
