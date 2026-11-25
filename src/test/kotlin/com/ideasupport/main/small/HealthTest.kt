package com.ideasupport.main.small

import com.ideasupport.infra.ai.AiClient
import com.ideasupport.main.common.health.HealthComponent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.jdbc.core.JdbcTemplate

class HealthTest {
	private val jdbc = mock<JdbcTemplate>()
	private val ai = mock<AiClient>()
	private val component = HealthComponent(jdbc, ai)

	@Test fun `healthy database and complete seed return ok`() {
		whenever(jdbc.queryForObject(any<String>(), eq(Int::class.java))).thenReturn(1, 3)
		whenever(ai.source).thenReturn("mock")
		assertEquals("ok", component.status().status)
	}

	@Test fun `incomplete seed degrades readiness without calling DB down`() {
		whenever(jdbc.queryForObject(any<String>(), eq(Int::class.java))).thenReturn(1, 2)
		whenever(ai.source).thenReturn("mock")
		val result = component.status()
		assertEquals("degraded", result.status)
		assertEquals("up", result.database)
		assertFalse(result.seedReady)
	}

	@Test fun `database failure returns degraded status instead of a stack trace`() {
		whenever(jdbc.queryForObject(any<String>(), eq(Int::class.java))).thenThrow(IllegalStateException("private database detail"))
		whenever(ai.source).thenReturn("gemini")
		val result = component.status()
		assertEquals("down", result.database)
		assertFalse(result.seedReady)
		assertEquals("gemini", result.aiMode)
	}
}
