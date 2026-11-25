package com.ideasupport.main.large.app.idea
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.infra.repository.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.jdbc.core.JdbcTemplate
import org.mockito.Mockito.*
import org.mockito.ArgumentMatchers.*
import java.util.UUID

@SpringBootTest(
	properties = [
		"spring.datasource.url=\${IDEA_TEST_JDBC_URL}", "spring.datasource.username=\${IDEA_TEST_DB_USER:root}", "spring.datasource.password=\${IDEA_TEST_DB_PASSWORD:}",
		"spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver", "spring.jpa.hibernate.ddl-auto=validate", "spring.liquibase.enabled=true", "spring.sql.init.mode=never",
		"app.limits.generation=1000", "app.limits.recipe=1000", "app.limits.preview=1000", "app.limits.category=1000",
	],
)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Tag("mysql")
abstract class MySqlApiTestSupport {
	@Autowired lateinit var mvc: MockMvc

	@Autowired lateinit var mapper: ObjectMapper

	@Autowired lateinit var words: WordRepository

	@Autowired lateinit var categories: CategoryRepository

	@Autowired lateinit var jdbc: JdbcTemplate

	@SpyBean lateinit var ai: AiClient

	protected fun request(
		method: String,
		path: String,
		body: Any? = null,
		token: String? = null,
		expected: Int = 200,
	): JsonNode {
		val builder =
			when (method) {
				"POST" -> post(path)
				"PUT" -> put(path)
				else -> get(path)
			}
		builder.contentType("application/json")
		if (body != null) builder.content(mapper.writeValueAsBytes(body))
		if (token != null) builder.header("Authorization", "Bearer $token")
		val result = mvc.perform(builder).andReturn().response
		assertEquals(expected, result.status, "$method $path: ${result.contentAsString}")
		return if (result.contentAsString.isBlank()) mapper.createObjectNode() else mapper.readTree(result.contentAsByteArray)
	}

	protected fun member(): Pair<String, String> {
		val id = "student-" + UUID.randomUUID().toString().take(10)
		request(
			"POST",
			"/app/user/sign-up",
			mapOf("id" to id, "pw" to "test-password", "email" to "$id@example.com", "name" to "테스트 학생", "birth" to "2000-01-01"),
		)
		val token = request("POST", "/app/user/login", mapOf("id" to id, "pw" to "test-password"))
		return token["accessToken"].asText() to token["refreshToken"].asText()
	}

	protected fun input(offset: Int = 0): Map<String, Any> {
		val category = categories.findAll().first { it.name == "음식 개발" }
		return mapOf(
			"categoryId" to category.id,
			"wordIds" to
				words.findCategoryWords(category.id).drop(offset).take(3).map {
					it.id
				},
			"direction" to "통합 테스트 ${UUID.randomUUID()}",
		)
	}

	@BeforeEach fun resetDedicatedDatabase() {
		check(jdbc.dataSource!!.connection.use { it.catalog }.endsWith("_test")) { "Refusing to clean a non-test database" }
		jdbc.update("UPDATE ideas SET parent_idea_id=NULL WHERE parent_idea_id IS NOT NULL")
		listOf("reactions", "posts", "recipes", "idea_words", "ideas").forEach { jdbc.update("DELETE FROM $it") }
		jdbc.update("DELETE cw FROM category_words cw JOIN categories c ON c.id=cw.category_id WHERE c.kind <> 'default'")
		jdbc.update("DELETE FROM categories WHERE kind <> 'default'")
		jdbc.update("DELETE FROM users")
	}
}
