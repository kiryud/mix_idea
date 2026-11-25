package com.ideasupport.main.app.word
import org.springframework.stereotype.Component
import com.ideasupport.core.entity.WordEntity
import com.ideasupport.infra.repository.WordRepository
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class WordComponent(private val words: WordRepository, private val policy: ContentPolicyComponent) {
	fun all(): List<WordEntity> = words.findAll()

	fun inCategory(categoryId: String): List<WordEntity> = words.findCategoryWords(categoryId)

	fun validate(
		categoryId: String,
		ids: List<String>,
		required: Int? = null,
	): List<WordEntity> {
		policy.requireInput(ids.distinct().size == ids.size && (required == null || ids.size == required))
		val found = inCategory(categoryId).associateBy { it.id }
		policy.requireInput(ids.all { it in found })
		return ids.map { found.getValue(it) }
	}

	fun draw(
		pool: List<WordEntity>,
		locked: List<WordEntity>,
		previous: List<String>,
	): List<WordEntity> {
		val candidates = pool.filter { candidate -> locked.none { it.id == candidate.id } }
		val needed = 3 - locked.size
		if (candidates.size < needed) throw ServerErrorException(INTERNAL_ERROR_CODE.WORDS_INSUFFICIENT)
		val preferred = candidates.filter { it.id !in previous }.shuffled()
		val rest = candidates.filter { it.id in previous }.shuffled()
		return locked + (preferred + rest).take(needed)
	}
}
