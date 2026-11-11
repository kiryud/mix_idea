package com.ideasupport.core.entity

import jakarta.persistence.*
import java.util.UUID

@Entity
@Table(name = "category_words", uniqueConstraints = [UniqueConstraint(columnNames = ["category_id", "word_id"])])
class CategoryWordEntity(
	@Id var id: String = UUID.randomUUID().toString(),
	@Column(name = "category_id") var categoryId: String = "",
	@Column(name = "word_id") var wordId: String = "",
	var score: Double = 1.0,
)
