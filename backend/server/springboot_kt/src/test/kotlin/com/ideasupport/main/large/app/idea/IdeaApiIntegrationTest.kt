package com.ideasupport.main.large.app.idea
import com.fasterxml.jackson.databind.JsonNode
import com.ideasupport.infra.repository.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class IdeaApiIntegrationTest : MySqlApiTestSupport() {
	@Test fun `complete member category generation recipe share feed reaction workflow`() {
		val (owner, refresh) = member()
		val (other, _) = member()
		request("GET", "/app/categories", expected = 401)
		request("GET", "/app/categories", token = refresh, expected = 401)
		assertTrue(request("GET", "/app/user/profile", token = owner)["uid"].asText().isNotBlank())
		assertTrue(request("POST", "/app/user/refresh-token", mapOf("refreshToken" to refresh))["accessToken"].asText().isNotBlank())
		assertTrue(request("GET", "/common/health")["seedReady"].asBoolean())
		val category = request("GET", "/app/categories", token = owner)["items"].first { it["name"].asText() == "음식 개발" }
		assertTrue(category["wordCount"].asInt() >= 100)
		val draw = request("POST", "/app/words/draw", mapOf("categoryId" to category["id"].asText()), owner)
		val locked = draw["words"][0]["id"].asText()
		val reroll = request("POST", "/app/words/draw", mapOf("categoryId" to category["id"].asText(), "lockedWordIds" to listOf(locked)), owner)
		assertTrue(reroll["words"].any { it["id"].asText() == locked && it["locked"].asBoolean() })
		request("POST", "/app/words/draw", mapOf("categoryId" to category["id"].asText(), "lockedWordIds" to listOf(locked, locked)), owner, 400)
		request("POST", "/app/categories/preview", mapOf("name" to "요리"), owner)
		val categorySuffix = UUID.randomUUID().toString().take(6)
		val custom = request("POST", "/app/categories", mapOf("name" to "캠핑 요리 $categorySuffix"), owner, 201)
		assertTrue(custom["category"]["wordCount"].asInt() >= 10)
		assertFalse(request("POST", "/app/categories", mapOf("name" to "캠핑요리$categorySuffix"), owner)["created"].asBoolean())
		val body = input()
		clearInvocations(ai)
		val result = request("POST", "/app/ideas", body, owner, 201)
		val id = result["idea"]["id"].asText()
		val reverse = body.toMutableMap().also { it["wordIds"] = (body["wordIds"] as List<*>).reversed() }
		assertTrue(request("POST", "/app/ideas", reverse, owner)["cached"].asBoolean())
		verify(ai, times(1)).generateIdea(anyString(), anyList(), anyString(), nullable(String::class.java))
		request("GET", "/app/ideas/$id", token = other)
		request("POST", "/app/ideas/$id/recipe", token = owner, expected = 201)
		assertTrue(request("POST", "/app/ideas/$id/recipe", token = owner)["cached"].asBoolean())
		request("POST", "/app/ideas/$id/recombine", token = owner, expected = 201)
		val link = request("POST", "/app/shares", mapOf("ideaId" to id), owner, 201)
		val html = mvc.perform(get("/s/${link["shareCode"].asText()}")).andReturn().response
		assertEquals(200, html.status)
		assertTrue(html.contentType!!.startsWith("text/html"))
		val post = request("POST", "/app/posts", mapOf("ideaId" to id), owner, 201)
		val postId = post["postId"].asText()
		assertFalse(request("POST", "/app/posts", mapOf("ideaId" to id), owner)["created"].asBoolean())
		request("PUT", "/app/posts/$postId/reaction", mapOf("kind" to "like"), owner, 403)
		assertEquals(1, request("PUT", "/app/posts/$postId/reaction", mapOf("kind" to "like"), other)["reactions"]["like"].asInt())
		assertEquals(1, request("PUT", "/app/posts/$postId/reaction", mapOf("kind" to "like"), other)["reactions"]["like"].asInt())
		val changed = request("PUT", "/app/posts/$postId/reaction", mapOf("kind" to "dislike"), other)
		assertEquals(0, changed["reactions"]["like"].asInt())
		assertEquals(1, changed["reactions"]["dislike"].asInt())
		assertTrue(request("PUT", "/app/posts/$postId/reaction", mapOf("kind" to null), other)["myReaction"].isNull)
		val feed = request("GET", "/app/posts?sort=popular&limit=1", token = other)
		assertTrue(feed["items"].size() > 0)
		assertFalse(feed["items"].any { it["id"].asText() == link["postId"].asText() })
		request("GET", "/app/posts?cursor=bad-cursor", token = other, expected = 400)
		jdbc.update("UPDATE ideas SET hidden=true WHERE id=?", id)
		request("GET", "/app/ideas/$id", token = other, expected = 404)
		assertEquals(404, mvc.perform(get("/s/${link["shareCode"].asText()}")).andReturn().response.status)
	}

	@Test fun `concurrent generation invokes AI once`() {
		val (token, _) = member()
		val body = input(5)
		clearInvocations(ai)
		val pool = Executors.newFixedThreadPool(5)
		try {
			val tasks =
				(1..5).map {
					pool.submit<JsonNode> {
						val builder =
							post(
								"/app/ideas",
							).contentType("application/json").header("Authorization", "Bearer $token").content(mapper.writeValueAsBytes(body))
						val response = mvc.perform(builder).andReturn().response
						assertTrue(response.status in listOf(200, 201), response.contentAsString)
						mapper.readTree(response.contentAsByteArray)
					}
				}
			assertEquals(1, tasks.map { it.get(15, TimeUnit.SECONDS)["idea"]["id"].asText() }.distinct().size)
			verify(ai, times(1)).generateIdea(anyString(), anyList(), anyString(), nullable(String::class.java))
		} finally {
			pool.shutdownNow()
		}
	}

	@Test fun `feed pagination excludes link-only and hidden records`() {
		val (owner, _) = member()
		val ids = (10..12).map { offset -> request("POST", "/app/ideas", input(offset), owner, 201)["idea"]["id"].asText() }
		ids.forEach { request("POST", "/app/posts", mapOf("ideaId" to it), owner, 201) }
		for (sort in listOf("latest", "popular")) {
			val first = request("GET", "/app/posts?sort=$sort&limit=1", token = owner)
			assertFalse(first["nextCursor"].isNull)
			val second = request("GET", "/app/posts?sort=$sort&limit=1&cursor=${first["nextCursor"].asText()}", token = owner)
			assertNotEquals(first["items"][0]["id"].asText(), second["items"][0]["id"].asText())
		}
	}

	@Test fun `invalid AI output is not saved and next request can recover`() {
		val (token, _) = member()
		val body = input(20)
		val before = jdbc.queryForObject("SELECT COUNT(*) FROM ideas", Long::class.java)
		doReturn(
			com.ideasupport.core.dto.app.idea.GeneratedIdeaDto("", ""),
		).`when`(ai).generateIdea(anyString(), anyList(), anyString(), nullable(String::class.java))
		request("POST", "/app/ideas", body, token, 502)
		assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM ideas", Long::class.java))
		doCallRealMethod().`when`(ai).generateIdea(anyString(), anyList(), anyString(), nullable(String::class.java))
		request("POST", "/app/ideas", body, token, 201)
	}

	@Test fun `direction validation documentation and malformed inputs`() {
		val (token, _) = member()
		val category = categories.findAll().first { it.name == "음식 개발" }
		val result = request("POST", "/app/words/draw", mapOf("categoryId" to category.id, "direction" to "매콤한"), token)
		assertEquals(3, result["words"].size())
		request("POST", "/app/words/draw", mapOf("categoryId" to category.id, "direction" to "무의미한입력xyz"), token, 422)
		request("POST", "/app/ideas", mapOf("categoryId" to category.id, "wordIds" to listOf("invalid", "other", "third")), token, 400)
		val docs = request("GET", "/v3/api-docs/app")
		assertTrue(docs["paths"].has("/app/ideas"))
		assertEquals("아이디어 생성", docs["paths"]["/app/ideas"]["post"]["summary"].asText())
	}
}
