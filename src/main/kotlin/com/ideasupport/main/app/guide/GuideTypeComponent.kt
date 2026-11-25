package com.ideasupport.main.app.guide

import com.ideasupport.core.entity.CategoryEntity
import com.ideasupport.infra.repository.CategoryRepository
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import org.springframework.stereotype.Component

enum class GuideType(val value: String, val label: String, val requirementsLabel: String) {
	FOOD("food", "만드는 법", "재료"),
	SOFTWARE("software", "구현 가이드", "개발 준비"),
	CONTEST("contest", "공모전 기획안", "기획 준비"),
	GENERAL("general", "실행 계획", "필요한 것"),
	;

	companion object {
		fun from(value: String): GuideType =
			entries.firstOrNull { it.value == value } ?: throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_INPUT)
	}
}

@Component
class GuideTypeComponent(private val categories: CategoryRepository) {
	fun of(category: CategoryEntity): GuideType = GuideType.from(category.guideType)

	fun forCategory(id: String): GuideType =
		of(
			categories.findById(id).orElseThrow {
				ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND)
			},
		)
}
