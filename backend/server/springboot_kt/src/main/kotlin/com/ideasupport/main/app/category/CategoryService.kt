package com.ideasupport.main.app.category
import org.springframework.stereotype.Service
import org.springframework.dao.DataIntegrityViolationException
import com.ideasupport.core.dto.app.category.*
import com.ideasupport.core.dto.app.word.WordOutDto
import com.ideasupport.main.app.user.UserIdentityComponent
import com.ideasupport.main.app.word.WordComponent
import com.ideasupport.main.app.word.WordMatchComponent
import com.ideasupport.lib.common.component.*
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.guide.GuideType

@Service
class CategoryService(
	private val category: CategoryComponent,
	private val word: WordComponent,
	private val match: WordMatchComponent,
	private val identity: UserIdentityComponent,
	private val policy: ContentPolicyComponent,
	private val limits: RequestLimitComponent,
) {
	fun list(): GetCategoriesOutDto = GetCategoriesOutDto(category.list())

	fun preview(input: CategoryNameInDto): PreviewCategoryOutDto {
		val user = identity.current()
		val name = policy.normalize(input.name)
		policy.requireInput(name.isNotBlank() && name.length <= 50)
		policy.check(name)
		category.find(name)?.let { existing ->
			val candidates = word.inCategory(existing.id)
			return PreviewCategoryOutDto(
				existing.name,
				existing.id,
				false,
				candidates.size,
				candidates.take(10).map {
					WordOutDto(it.id, it.text)
				},
				"cache",
				existing.guideType,
				GuideType.from(existing.guideType).label,
			)
		}
		limits.check("preview", user.idx!!)
		val result = match.match(name, word.all(), 10, user.idx!!)
		if (result.words.size < 10) throw ServerErrorException(INTERNAL_ERROR_CODE.CATEGORY_MATCH_INSUFFICIENT)
		val type = GuideType.from(input.guideType)
		return PreviewCategoryOutDto(
			name,
			null,
			true,
			result.words.size,
			result.words.take(10).map {
				WordOutDto(it.id, it.text)
			},
			result.source,
			type.value,
			type.label,
		)
	}

	fun create(input: CategoryNameInDto): CreateCategoryOutDto {
		val user = identity.current()
		val preview = preview(input)
		category.find(preview.name)?.let { return CreateCategoryOutDto(category.output(it), false) }
		val candidates = match.match(preview.name, word.all(), 10, user.idx!!).words
		if (candidates.size < 10) throw ServerErrorException(INTERNAL_ERROR_CODE.CATEGORY_MATCH_INSUFFICIENT)
		limits.check("category", user.idx!!)
		return try {
			CreateCategoryOutDto(category.output(category.create(preview.name, candidates, user.idx!!, preview.guideType)), true)
		} catch (ex: DataIntegrityViolationException) {
			val existing = category.find(preview.name) ?: throw ex
			CreateCategoryOutDto(category.output(existing), false)
		}
	}
}
