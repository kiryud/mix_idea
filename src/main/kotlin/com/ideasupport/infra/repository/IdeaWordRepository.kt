package com.ideasupport.infra.repository
import com.ideasupport.core.entity.IdeaWordEntity
import org.springframework.data.jpa.repository.*

interface IdeaWordRepository : JpaRepository<IdeaWordEntity, String> {
	fun findByIdeaIdIn(ideaIds: List<String>): List<IdeaWordEntity>

	fun findByIdeaIdOrderByPositionAsc(ideaId: String): List<IdeaWordEntity>
}
