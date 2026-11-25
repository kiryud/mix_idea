package com.ideasupport.infra.repository
import com.ideasupport.core.entity.WordEntity
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param

interface WordRepository : JpaRepository<WordEntity, String> {
	fun findByTextKey(textKey: String): WordEntity?

	@Query("select w from WordEntity w, CategoryWordEntity cw where cw.wordId = w.id and cw.categoryId = :categoryId order by w.id")
	fun findCategoryWords(
		@Param("categoryId") categoryId: String,
	): List<WordEntity>
}
