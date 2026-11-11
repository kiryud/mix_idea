package com.ideasupport.core.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "reactions", uniqueConstraints = [UniqueConstraint(columnNames = ["post_id", "user_idx"])])
class ReactionEntity(
	@Id var id: String = UUID.randomUUID().toString(),
	@Column(name = "post_id") var postId: String = "",
	@Column(name = "user_idx") var userIdx: Long = 0,
	@Column(length = 16) var kind: String = "like",
	@Column(name = "updated_at") var updatedAt: Instant = Instant.now(),
)
