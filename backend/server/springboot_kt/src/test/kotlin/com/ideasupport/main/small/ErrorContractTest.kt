package com.ideasupport.main.small

import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.lib.common.handler.GlobalServerErrorExceptionHandler
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest

class ErrorContractTest {
	private val handler = GlobalServerErrorExceptionHandler()

	@Test fun `rate limit includes retry after and stable JSON fields`() {
		val response = handler.domain(ServerErrorException(INTERNAL_ERROR_CODE.RATE_LIMITED, 42))
		assertEquals(429, response.statusCode.value())
		assertEquals("42", response.headers.getFirst("Retry-After"))
		assertEquals(1107, response.body!!.code)
		assertEquals("RATE_LIMITED", response.body!!.name)
	}

	@Test fun `rate limit without an interval uses sixty seconds`() {
		assertEquals("60", handler.domain(ServerErrorException(INTERNAL_ERROR_CODE.RATE_LIMITED)).headers.getFirst("Retry-After"))
	}

	@Test fun `input failure is 400 with domain JSON`() {
		assertEquals(1100, handler.input(IllegalArgumentException()).body!!.code)
	}

	@Test fun `missing resource is 404`() {
		assertEquals(404, handler.missing().statusCode.value())
	}

	@Test fun `unexpected exceptions never expose private messages`() {
		val result = handler.unexpected(IllegalStateException("private-provider-key"), MockHttpServletRequest("POST", "/app/ideas"))
		assertEquals(500, result.statusCode.value())
		assertEquals(9997, result.body!!.code)
		assertFalse(result.body.toString().contains("private-provider-key"))
	}
}
