package com.ideasupport.core.entity

import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "ideas")
class IdeaEntity(
	@Id var id: String = UUID.randomUUID().toString(),
	@Column(name = "category_id") var categoryId: String = "",
	@Column(name = "combo_key", unique = true, length = 128) var comboKey: String = "",
	@Column(length = 200) var direction: String = "",
	@Column(length = 160) var title: String = "",
	@Column(columnDefinition = "TEXT") var content: String = "",
	@Column(name = "parent_idea_id") var parentIdeaId: String? = null,
	@Column(length = 16) var source: String = "mock",
	var hidden: Boolean = false,
	@Column(name = "created_at") var createdAt: Instant = Instant.now(),
)
