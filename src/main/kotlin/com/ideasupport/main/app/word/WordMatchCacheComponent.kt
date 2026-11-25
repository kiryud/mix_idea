package com.ideasupport.main.app.word
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Instant
import com.ideasupport.core.entity.*
import com.ideasupport.infra.repository.MatchCacheRepository
import com.ideasupport.lib.common.component.*

@Component
class WordMatchCacheComponent(private val cache: MatchCacheRepository, private val mapper: ObjectMapper) {
	fun get(key: String): List<String>? =
		cache.findById(key).orElse(null)?.takeIf {
			it.expiresAt.isAfter(Instant.now())
		}?.let { mapper.readValue<List<String>>(it.result) }

	@Transactional fun put(
		key: String,
		input: String,
		ids: List<String>,
	) {
		cache.save(MatchCacheEntity(key, input, mapper.writeValueAsString(ids), Instant.now().plusSeconds(86400)))
	}
}
