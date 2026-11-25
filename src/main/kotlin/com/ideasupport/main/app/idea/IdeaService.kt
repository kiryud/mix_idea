package com.ideasupport.main.app.idea
import org.springframework.stereotype.Service
import org.springframework.dao.DataIntegrityViolationException
import java.util.UUID
import com.ideasupport.core.dto.app.idea.*
import com.ideasupport.main.app.category.CategoryComponent
import com.ideasupport.main.app.word.WordComponent
import com.ideasupport.main.app.user.UserIdentityComponent
import com.ideasupport.lib.common.component.*

@Service
class IdeaService(
	private val storage: IdeaStorageComponent,
	private val generation: IdeaGenerationComponent,
	private val category: CategoryComponent,
	private val word: WordComponent,
	private val identity: UserIdentityComponent,
	private val coordinator: GenerationCoordinatorComponent,
	private val limits: RequestLimitComponent,
	private val policy: ContentPolicyComponent,
) {
	fun get(id: String) = GetIdeaOutDto(storage.output(storage.get(id)))

	fun create(input: CreateIdeaInDto): CreateIdeaOutDto {
		val user = identity.current()
		val selectedCategory = category.get(input.categoryId)
		val words = word.validate(input.categoryId, input.wordIds, 3)
		val direction = policy.normalize(input.direction)
		policy.requireInput(direction.length <= 200)
		policy.check(direction)
		val key = generation.key(input.categoryId, input.wordIds, direction)
		storage.cached(key)?.let { return CreateIdeaOutDto(storage.output(it), true) }
		var created = false
		val result =
			coordinator.execute("idea:$key") {
				storage.cached(key) ?: run {
					limits.check("generation", user.idx!!)
					val generated = generation.generate(selectedCategory.name, words.map { it.text }, direction, null)
					try {
						storage.save(input.categoryId, key, direction, generation.source, generated, words, null).also { created = true }
					} catch (ex: DataIntegrityViolationException) {
						storage.cached(key) ?: throw ex
					}
				}
			}
		return CreateIdeaOutDto(storage.output(result), !created)
	}

	fun recombine(id: String): CreateIdeaOutDto {
		val user = identity.current()
		val original = storage.get(id)
		val words = storage.wordEntities(id)
		val selectedCategory = category.get(original.categoryId)
		limits.check("generation", user.idx!!)
		val variation = UUID.randomUUID().toString()
		val generated = generation.generate(selectedCategory.name, words.map { it.text }, original.direction, variation)
		val result =
			storage.save(
				original.categoryId,
				generation.key(
					original.categoryId,
					words.map {
						it.id
					},
					original.direction,
				) + ":" + variation,
				original.direction,
				generation.source,
				generated,
				words,
				id,
			)
		return CreateIdeaOutDto(storage.output(result), false)
	}
}
