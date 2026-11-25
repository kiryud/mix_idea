package com.ideasupport.main.small
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import com.ideasupport.lib.common.component.RequestLimitComponent
import com.ideasupport.lib.common.exception.ServerErrorException

class RequestLimitTest {
	@Test fun `exceeded member limit returns retry interval`() {
		val limits = RequestLimitComponent(1, 1, 1, 1)
		limits.check("generation", 1)
		val error = assertThrows(ServerErrorException::class.java) { limits.check("generation", 1) }
		assertEquals(1107, error.errorCode)
		assertTrue(error.retryAfterSeconds!! in 1..60)
	}

	@Test fun `actions and members have independent budgets`() {
		val limits = RequestLimitComponent(1, 1, 1, 1)
		limits.check("generation", 1)
		assertDoesNotThrow {
			limits.check("recipe", 1)
			limits.check("generation", 2)
		}
	}

	@Test fun `shared IP has a bounded aggregate budget`() {
		val limits = RequestLimitComponent(1, 1, 1, 1)
		(1L..3L).forEach { limits.check("generation", it) }
		assertThrows(ServerErrorException::class.java) { limits.check("generation", 4) }
	}

	@Test fun `category creation uses a daily retry window`() {
		val limits = RequestLimitComponent(1, 1, 1, 1)
		limits.check("category", 1)
		val error = assertThrows(ServerErrorException::class.java) { limits.check("category", 1) }
		assertTrue(error.retryAfterSeconds!! in 86395..86400)
	}
}
