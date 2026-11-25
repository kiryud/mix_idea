package com.ideasupport.main.small

import com.ideasupport.core.entity.WordEntity
import com.ideasupport.infra.repository.WordRepository
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.word.WordComponent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.Mockito.*

class WordComponentTest {
	private val repository = mock(WordRepository::class.java)
	private val component = WordComponent(repository, ContentPolicyComponent())
	private val pool = (1..8).map { WordEntity(id = "w$it", text = "단어$it") }

	@Test fun `validation preserves requested order`() {
		`when`(repository.findCategoryWords("c")).thenReturn(pool)
		assertEquals(listOf("w3", "w1", "w2"), component.validate("c", listOf("w3", "w1", "w2"), 3).map { it.id })
	}

	@TestFactory fun `wrong count duplicate and foreign IDs are rejected`() =
		listOf(listOf("w1"), listOf("w1", "w1", "w2"), listOf("w1", "w2", "foreign")).map { ids ->
			dynamicTest(ids.toString()) {
				`when`(repository.findCategoryWords("c")).thenReturn(pool)
				assertEquals(1100, assertThrows(ServerErrorException::class.java) { component.validate("c", ids, 3) }.errorCode)
			}
		}

	@Test fun `draw returns three distinct members of the pool`() {
		val result = component.draw(pool, emptyList(), emptyList())
		assertEquals(3, result.map { it.id }.distinct().size)
		assertTrue(result.all { it in pool })
	}

	@Test fun `locked words survive and are never drawn twice`() {
		val result = component.draw(pool, pool.take(2), emptyList())
		assertEquals(pool.take(2), result.take(2))
		assertEquals(3, result.map { it.id }.distinct().size)
	}

	@Test fun `reroll excludes previous words when enough alternatives exist`() {
		assertTrue(component.draw(pool, emptyList(), pool.take(3).map { it.id }).none { it in pool.take(3) })
	}

	@Test fun `small pool reuses previous words instead of losing a slot`() {
		assertEquals(3, component.draw(pool.take(3), emptyList(), pool.take(3).map { it.id }).size)
	}

	@Test fun `insufficient pool returns a domain error`() {
		assertEquals(1104, assertThrows(ServerErrorException::class.java) { component.draw(pool.take(2), emptyList(), emptyList()) }.errorCode)
	}
}
