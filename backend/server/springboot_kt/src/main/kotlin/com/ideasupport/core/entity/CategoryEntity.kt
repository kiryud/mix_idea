package com.ideasupport.core.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "categories")
class CategoryEntity(
	@Id var id: String = UUID.randomUUID().toString(),
	@Column(length = 50) var name: String = "",
	@Column(name = "name_key", unique = true, length = 150) var nameKey: String = "",
	@Column(length = 16) var kind: String = "user",
	@Column(name = "created_by") var createdBy: Long? = null,
	@Column(name = "use_count") var useCount: Long = 0,
	@Column(name = "created_at") var createdAt: Instant = Instant.now(),
	@Column(name = "guide_type", length = 16) var guideType: String = "general",
)
