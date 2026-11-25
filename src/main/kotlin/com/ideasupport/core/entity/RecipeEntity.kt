package com.ideasupport.core.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "recipes")
class RecipeEntity(
	@Id @Column(name = "idea_id") var ideaId: String = "",
	@Column(length = 16) var kind: String = "plan",
	var feasible: Boolean = true,
	@Column(columnDefinition = "TEXT") var summary: String = "",
	@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "json") var ingredients: String = "[]",
	@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "json") var steps: String = "[]",
	@Column(length = 16) var source: String = "mock",
	@Column(name = "created_at") var createdAt: Instant = Instant.now(),
)
