package com.ideasupport.main.app.word
import org.springframework.stereotype.Service
import com.ideasupport.core.dto.app.word.*
import com.ideasupport.main.app.category.CategoryComponent
import com.ideasupport.main.app.user.UserIdentityComponent
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Service
class WordService(
	private val words: WordComponent,
	private val match: WordMatchComponent,
	private val categories: CategoryComponent,
	private val identity: UserIdentityComponent,
	private val policy: ContentPolicyComponent,
) {
	fun draw(input: DrawWordsInDto): DrawWordsOutDto {
		val user = identity.current()
		categories.get(input.categoryId)
		policy.requireInput(input.lockedWordIds.size <= 2 && input.previousWordIds.size <= 3)
		val locked = words.validate(input.categoryId, input.lockedWordIds)
		words.validate(input.categoryId, input.previousWordIds)
		val direction = policy.normalize(input.direction)
		policy.requireInput(direction.length <= 200)
		val pool = words.inCategory(input.categoryId)
		val selected = if (direction.isBlank()) WordMatchResult(pool, "random") else match.match(direction, pool, 3 - locked.size, user.idx!!)
		if (direction.isNotBlank() && selected.words.count {
					candidate ->
				locked.none { it.id == candidate.id }
			} < 3 - locked.size
		) {
			throw ServerErrorException(INTERNAL_ERROR_CODE.DIRECTION_TOO_NARROW)
		}
		val result = words.draw(selected.words, locked, input.previousWordIds)
		categories.recordUse(input.categoryId)
		return DrawWordsOutDto(result.map { DrawWordOutDto(it.id, it.text, it.id in input.lockedWordIds) }, direction, selected.source)
	}
}
