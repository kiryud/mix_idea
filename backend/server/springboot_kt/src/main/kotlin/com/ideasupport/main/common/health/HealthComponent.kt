package com.ideasupport.main.common.health
import org.springframework.stereotype.Component
import org.springframework.jdbc.core.JdbcTemplate
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.core.dto.common.basic.HealthOutDto

@Component
class HealthComponent(private val jdbc: JdbcTemplate, private val ai: AiClient) {
	fun status(): HealthOutDto =
		try {
			jdbc.queryForObject("SELECT 1", Int::class.java)
			val ready = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT c.id FROM categories c JOIN category_words cw ON cw.category_id=c.id WHERE c.kind='default' GROUP BY c.id HAVING COUNT(*) >= 100) seeded", Int::class.java)!! >= 3
			HealthOutDto(if (ready) "ok" else "degraded", "up", ai.source, ready)
		} catch (ex: Exception) {
			HealthOutDto("degraded", "down", ai.source, false)
		}
}
