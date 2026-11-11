package com.ideasupport.infra.repository
import com.ideasupport.core.entity.PostEntity
import org.springframework.data.jpa.repository.*
import org.springframework.data.repository.query.Param

interface PostRepository : JpaRepository<PostEntity, String> {
	fun findByIdeaIdAndUserIdxAndScope(
		ideaId: String,
		userIdx: Long,
		scope: String,
	): PostEntity?

	fun findByShareCodeAndHiddenFalse(shareCode: String): PostEntity?

	@Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from PostEntity p where p.id = :id")
	fun findForUpdate(
		@Param("id") id: String,
	): PostEntity?
}
