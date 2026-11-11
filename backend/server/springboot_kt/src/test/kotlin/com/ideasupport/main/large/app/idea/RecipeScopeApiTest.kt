package com.ideasupport.main.large.app.idea

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.*
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import java.util.UUID

class RecipeScopeApiTest : MySqlApiTestSupport() {
	@Test fun `category metadata advertises cooking only for the seeded food category`() {
		val (token, _) = member()
		val items = request("GET", "/app/categories", token = token)["items"]
		assertTrue(items.first { it["name"].asText() == "음식 개발" }["supportsRecipe"].asBoolean())
		assertTrue(items.filter { it["name"].asText() != "음식 개발" }.all { !it["supportsRecipe"].asBoolean() })
		val custom = request("POST", "/app/categories", mapOf("name" to "캠핑 요리 ${UUID.randomUUID()}"), token, 201)
		assertFalse(custom["category"]["supportsRecipe"].asBoolean())
	}

	private fun ideaFor(
		name: String,
		token: String,
	): String {
		val category = categories.findAll().first { it.name == name }
		return request(
			"POST",
			"/app/ideas",
			mapOf(
				"categoryId" to category.id,
				"wordIds" to
					words.findCategoryWords(category.id).take(3).map {
						it.id
					},
				"direction" to UUID.randomUUID().toString(),
			),
			token,
			201,
		)["idea"]["id"].asText()
	}

	private fun legacyPlan(id: String) {
		jdbc.update(
			"INSERT INTO recipes(idea_id,kind,feasible,summary,ingredients,steps,source,created_at) VALUES(?,'plan',true,'LEGACY_EXECUTION_PLAN','[]','[]','mock',CURRENT_TIMESTAMP(6))",
			id,
		)
	}

	@TestFactory fun `program and contest receive their own follow-up guidance`() =
		listOf("프로그램 개발", "공모전 주제선정").map { name ->
			dynamicTest(name) {
				val (token, _) = member()
				val id = ideaFor(name, token)
				clearInvocations(ai)
				val result = request("POST", "/app/ideas/$id/recipe", token = token, expected = 201)
				assertEquals(if (name == "프로그램 개발") "software" else "contest", result["recipe"]["kind"].asText())
				verify(ai, never()).generateRecipe(anyString(), anyString(), anyString())
			}
		}

	@Test fun `legacy untyped plan is replaced and shared with the correct heading`() {
		val (token, _) = member()
		val id = ideaFor("공모전 주제선정", token)
		legacyPlan(id)
		val idea = request("GET", "/app/ideas/$id", token = token)["idea"]
		assertFalse(idea["hasRecipe"].asBoolean())
		assertTrue(idea.has("supportsRecipe"))
		assertFalse(idea["supportsRecipe"].asBoolean())
		request("POST", "/app/ideas/$id/recipe", token = token, expected = 201)
		val link = request("POST", "/app/shares", mapOf("ideaId" to id), token, 201)
		val response = mvc.perform(get("/s/${link["shareCode"].asText()}")).andReturn().response
		assertEquals(200, response.status)
		assertFalse(response.contentAsString.contains("만드는 법"))
		assertFalse(response.contentAsString.contains("LEGACY_EXECUTION_PLAN"))
		assertTrue(response.getContentAsString(Charsets.UTF_8).contains("<h2>공모전 기획안</h2>"))
		assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM recipes WHERE idea_id=?", Int::class.java, id))
	}

	@Test fun `food advertises support and a legacy plan is replaced by a real cooking recipe`() {
		val (token, _) = member()
		val id = ideaFor("음식 개발", token)
		legacyPlan(id)
		val idea = request("GET", "/app/ideas/$id", token = token)["idea"]
		assertTrue(idea.path("supportsRecipe").asBoolean())
		assertFalse(idea["hasRecipe"].asBoolean())
		val result = request("POST", "/app/ideas/$id/recipe", token = token, expected = 201)
		assertEquals("food", result["recipe"]["kind"].asText())
		assertTrue(request("POST", "/app/ideas/$id/recipe", token = token)["cached"].asBoolean())
	}
}
