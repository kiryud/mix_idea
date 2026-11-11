package com.ideasupport.core.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "posts", uniqueConstraints = [UniqueConstraint(columnNames = ["idea_id", "user_idx", "scope"])])
class PostEntity(
	@Id var id: String = UUID.randomUUID().toString(),
	@Column(name = "idea_id") var ideaId: String = "",
	@Column(name = "user_idx") var userIdx: Long = 0,
	@Column(length = 16) var scope: String = "link",
	@Column(name = "share_code", unique = true, length = 64) var shareCode: String = "",
	var hidden: Boolean = false,
	@Column(name = "created_at") var createdAt: Instant = Instant.now(),
)
