package com.ideasupport.main.small

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ideasupport.core.entity.*
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.infra.repository.MatchCacheRepository
import com.ideasupport.lib.common.component.*
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.main.app.word.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.*
import java.time.Instant
import java.util.Optional

class WordMatchTest {
	private val ai = mock<AiClient>()
	private val repository = mock<MatchCacheRepository>()
	private val mapper = jacksonObjectMapper()
	private val cache = WordMatchCacheComponent(repository, mapper)
	private val component =
		WordMatchComponent(ai, ContentPolicyComponent(), cache, GenerationCoordinatorComponent(), RequestLimitComponent(100, 100, 100, 100))
	private val pool = (1..15).map { WordEntity(id = "w$it", text = "단어$it") }

	private fun provider() {
		whenever(ai.source).thenReturn("gemini")
		whenever(ai.cacheVersion).thenReturn("test-v1")
		whenever(repository.findById(any())).thenReturn(Optional.empty())
	}

	@Test fun `sufficient keyword matches avoid AI entirely`() {
		provider()
		val tagged = pool.map { WordEntity(id = it.id, text = it.text, keywords = "캠핑 요리") }
		val result = component.match("캠핑", tagged, 10, 1)
		assertEquals("keyword", result.source)
		assertEquals(15, result.words.size)
		verify(ai, never()).selectWords(any(), any())
	}

	@Test fun `mock mode never invents matching words`() {
		whenever(ai.source).thenReturn("mock")
		assertTrue(component.match("unrelated", pool, 10, 1).words.isEmpty())
		verify(ai, never()).selectWords(any(), any())
	}

	@Test fun `valid provider IDs are cached and returned from candidate pool`() {
		provider()
		whenever(ai.selectWords(any(), any())).thenReturn(pool.take(10).map { it.id })
		val result = component.match("건강 관리", pool, 10, 1)
		assertEquals("ai", result.source)
		assertEquals(pool.take(10).map { it.id }.toSet(), result.words.map { it.id }.toSet())
		verify(repository).save(check<MatchCacheEntity> { assertEquals(10, mapper.readTree(it.result).size()) })
	}

	@TestFactory fun `duplicate foreign and excessive AI selections are rejected without caching`() =
		listOf(listOf("w1", "w1"), listOf("foreign"), (1..41).map { "w$it" }).map { ids ->
			dynamicTest("invalid selection size ${ids.size}") {
				provider()
				whenever(ai.selectWords(any(), any())).thenReturn(ids)
				assertEquals(1108, assertThrows(ServerErrorException::class.java) { component.match("건강 관리", pool, 10, 1) }.errorCode)
				verify(repository, never()).save(any())
			}
		}

	@Test fun `fresh cache avoids provider and maps IDs back to current words`() {
		provider()
		whenever(
			repository.findById(any()),
		).thenReturn(Optional.of(MatchCacheEntity(result = "[\"w1\",\"w2\"]", expiresAt = Instant.now().plusSeconds(60))))
		val result = component.match("건강 관리", pool, 3, 1)
		assertEquals("cache", result.source)
		assertEquals(pool.take(2), result.words)
		verify(ai, never()).selectWords(any(), any())
	}

	@Test fun `expired cache is ignored`() {
		whenever(repository.findById("old")).thenReturn(Optional.of(MatchCacheEntity(expiresAt = Instant.now().minusSeconds(1))))
		assertNull(cache.get("old"))
	}

	@Test fun `cache key changes when candidate metadata changes`() {
		provider()
		whenever(ai.selectWords(any(), any())).thenReturn(emptyList())
		component.match("건강", pool, 3, 1)
		component.match("건강", pool.map { WordEntity(id = it.id, text = "바뀐${it.text}") }, 3, 1)
		val saved = argumentCaptor<MatchCacheEntity>()
		verify(repository, times(2)).save(saved.capture())
		assertNotEquals(saved.allValues[0].cacheKey, saved.allValues[1].cacheKey)
	}
}
