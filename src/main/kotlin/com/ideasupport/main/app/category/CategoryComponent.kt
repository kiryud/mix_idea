package com.ideasupport.main.app.category
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import com.ideasupport.core.entity.*
import com.ideasupport.core.dto.app.category.*
import com.ideasupport.infra.repository.*
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.guide.GuideTypeComponent
import com.ideasupport.main.app.guide.GuideType

@Component
class CategoryComponent(
	private val categories: CategoryRepository,
	private val links: CategoryWordRepository,
	private val policy: ContentPolicyComponent,
	private val guideTypes: GuideTypeComponent,
) {
	fun get(id: String): CategoryEntity = categories.findById(id).orElseThrow { ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND) }

	fun find(name: String): CategoryEntity? = categories.findByNameKey(policy.key(name))

	fun list(): List<CategoryOutDto> = categories.findAllByOrderByKindAscUseCountDescIdAsc().map { output(it) }

	fun output(category: CategoryEntity) =
		CategoryOutDto(
			category.id,
			category.name,
			category.kind,
			links.countByCategoryId(category.id),
			category.useCount,
			guideTypes.of(category) == GuideType.FOOD,
			category.guideType,
			guideTypes.of(category).label,
		)

	@Transactional fun create(
		name: String,
		words: List<WordEntity>,
		userIdx: Long,
		guideType: String = "general",
	): CategoryEntity {
		val category =
			categories.saveAndFlush(
				CategoryEntity(name = name, nameKey = policy.key(name), createdBy = userIdx, guideType = GuideType.from(guideType).value),
			)
		links.saveAll(words.map { CategoryWordEntity(categoryId = category.id, wordId = it.id) })
		return category
	}

	@Transactional fun recordUse(id: String) {
		categories.incrementUseCount(id)
	}
}
