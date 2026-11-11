package com.ideasupport.infra.repository
import com.ideasupport.core.entity.IdeaEntity
import org.springframework.data.jpa.repository.*

interface IdeaRepository : JpaRepository<IdeaEntity, String> {
	fun findByComboKey(comboKey: String): IdeaEntity?
}
