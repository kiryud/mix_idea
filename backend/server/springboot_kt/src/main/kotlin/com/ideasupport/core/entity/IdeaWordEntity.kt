package com.ideasupport.core.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(
	name = "idea_words",
	uniqueConstraints = [UniqueConstraint(columnNames = ["idea_id", "word_id"]), UniqueConstraint(columnNames = ["idea_id", "position"])],
)
class IdeaWordEntity(
	@Id var id: String = UUID.randomUUID().toString(),
	@Column(name = "idea_id") var ideaId: String = "",
	@Column(name = "word_id") var wordId: String = "",
	var position: Int = 0,
)
