package com.ideasupport.main.app.idea
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import com.ideasupport.core.entity.*
import com.ideasupport.core.dto.app.idea.*
import com.ideasupport.core.dto.app.word.WordOutDto
import com.ideasupport.infra.repository.*
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.guide.GuideTypeComponent
import com.ideasupport.main.app.guide.GuideType

@Component
class IdeaStorageComponent(
	private val ideas: IdeaRepository,
	private val links: IdeaWordRepository,
	private val words: WordRepository,
	private val recipes: RecipeRepository,
	private val guideTypes: GuideTypeComponent,
) {
	fun cached(key: String): IdeaEntity? =
		ideas.findByComboKey(key)?.also { if (it.hidden) throw ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND) }

	fun get(id: String): IdeaEntity =
		ideas.findById(id).orElse(null)?.takeUnless {
			it.hidden
		} ?: throw ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND)

	fun wordEntities(id: String): List<WordEntity> {
		val rows = links.findByIdeaIdOrderByPositionAsc(id)
		val found = words.findAllById(rows.map { it.wordId }).associateBy { it.id }
		return rows.map { found.getValue(it.wordId) }
	}

	fun output(idea: IdeaEntity): IdeaOutDto {
		val type = guideTypes.forCategory(idea.categoryId)
		val hasGuide = recipes.findById(idea.id).orElse(null)?.kind == type.value
		return IdeaOutDto(
			idea.id, idea.categoryId,
			wordEntities(idea.id).map {
				WordOutDto(it.id, it.text)
			},
			idea.direction, idea.title, idea.content, idea.parentIdeaId, idea.source,
			type == GuideType.FOOD && hasGuide,
			idea.createdAt, type == GuideType.FOOD,
			type.value, type.label, hasGuide,
		)
	}

	@Transactional fun save(
		categoryId: String,
		key: String,
		direction: String,
		source: String,
		result: GeneratedIdeaDto,
		selected: List<WordEntity>,
		parentId: String?,
	): IdeaEntity {
		val idea =
			ideas.saveAndFlush(
				IdeaEntity(
					categoryId = categoryId,
					comboKey = key,
					direction = direction,
					title = result.title,
					content = result.content,
					parentIdeaId = parentId,
					source = source,
				),
			)
		links.saveAll(selected.mapIndexed { index, word -> IdeaWordEntity(ideaId = idea.id, wordId = word.id, position = index) })
		return idea
	}
}
