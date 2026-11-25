package com.ideasupport.infra.repository
import com.ideasupport.core.entity.CategoryEntity
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param

interface CategoryRepository : JpaRepository<CategoryEntity, String> {
	fun findByNameKey(nameKey: String): CategoryEntity?

	@Modifying
	@Query("update CategoryEntity c set c.useCount = c.useCount + 1 where c.id = :id")
	fun incrementUseCount(
		@Param("id") id: String,
	): Int

	fun findAllByOrderByKindAscUseCountDescIdAsc(): List<CategoryEntity>
}
