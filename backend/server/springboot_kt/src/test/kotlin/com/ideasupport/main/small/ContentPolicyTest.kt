package com.ideasupport.main.small

import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.exception.ServerErrorException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory

class ContentPolicyTest {
	private val policy = ContentPolicyComponent()

	@Test fun `compatibility characters and whitespace are normalized`() {
		assertEquals("ABC 건강 관리", policy.normalize("  ＡＢＣ\t건강\n관리  "))
	}

	@Test fun `name keys ignore case spaces and compatibility characters`() {
		assertEquals(policy.key("AI 건강 관리"), policy.key("ａｉ건강관리"))
	}

	@Test fun `hash is deterministic UTF8 SHA256`() {
		assertEquals(64, policy.hash("감자").length)
		assertEquals(policy.hash("감자"), policy.hash("감자"))
		assertNotEquals(policy.hash("감자"), policy.hash("고구마"))
	}

	@Test fun `ordinary creative content is accepted`() {
		assertDoesNotThrow { policy.check("학생을 위한 건강한 요리 프로그램") }
	}

	@Test fun `invalid input maps to public domain error`() {
		assertEquals(1100, assertThrows(ServerErrorException::class.java) { policy.requireInput(false) }.errorCode)
	}

	@TestFactory fun `unsafe normalized phrases are rejected`() =
		listOf("아동 성착취", "폭탄제조", "자살  방법").map { value ->
			dynamicTest(value) { assertEquals(1106, assertThrows(ServerErrorException::class.java) { policy.check(value) }.errorCode) }
		}
}
