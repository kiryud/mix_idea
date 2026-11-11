package com.ideasupport.infra.repository
import com.ideasupport.core.entity.ReactionEntity
import org.springframework.data.jpa.repository.*

interface ReactionRepository : JpaRepository<ReactionEntity, String> {
	fun findByPostIdAndUserIdx(
		postId: String,
		userIdx: Long,
	): ReactionEntity?

	fun countByPostIdAndKind(
		postId: String,
		kind: String,
	): Long
}
