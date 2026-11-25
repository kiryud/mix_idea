package com.ideasupport.core.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "words")
class WordEntity(
	@Id var id: String = UUID.randomUUID().toString(),
	@Column(length = 150) var text: String = "",
	@Column(name = "text_key", unique = true, length = 150) var textKey: String = "",
	@Column(columnDefinition = "TEXT") var keywords: String = "",
)
