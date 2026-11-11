package com.ideasupport.infra.repository
import com.ideasupport.core.entity.CategoryWordEntity
import org.springframework.data.jpa.repository.*

interface CategoryWordRepository : JpaRepository<CategoryWordEntity, String> {
	fun countByCategoryId(categoryId: String): Long

	fun existsByCategoryIdAndWordId(
		categoryId: String,
		wordId: String,
	): Boolean
}
