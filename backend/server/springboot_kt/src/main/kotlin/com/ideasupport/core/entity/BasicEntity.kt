package com.ideasupport.core.entity

import jakarta.persistence.Column
import java.time.Instant
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate

open class BasicEntity(
	@CreatedDate
	@Column(name = "created_at")
	open var createdAt: Instant? = Instant.now(),
	@LastModifiedDate
	@Column(name = "updated_at")
	open var updatedAt: Instant? = null,
)
