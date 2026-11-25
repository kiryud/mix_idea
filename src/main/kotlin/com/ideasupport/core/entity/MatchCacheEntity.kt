package com.ideasupport.core.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(name = "match_cache")
class MatchCacheEntity(
	@Id @Column(name = "cache_key", length = 64) var cacheKey: String = "",
	@Column(name = "input_text", length = 200) var inputText: String = "",
	@JdbcTypeCode(SqlTypes.JSON) @Column(columnDefinition = "json") var result: String = "[]",
	@Column(name = "expires_at") var expiresAt: Instant = Instant.now(),
)
