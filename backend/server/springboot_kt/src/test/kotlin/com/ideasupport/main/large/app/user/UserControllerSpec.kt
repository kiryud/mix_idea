package com.ideasupport.main.large.app.user
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerSpec {
	@Autowired lateinit var mvc: MockMvc

	@Autowired lateinit var mapper: ObjectMapper

	@Test fun `own profile uses existing login tokens and ordinary user role`() {
		val id = UUID.randomUUID().toString()
		val body =
			mapper.writeValueAsBytes(
				mapOf("id" to id, "pw" to "test-password", "email" to "student@example.com", "name" to "학생", "birth" to "2000-01-01"),
			)
		assertEquals(200, mvc.perform(post("/app/user/sign-up").contentType("application/json").content(body)).andReturn().response.status)
		val login =
			mvc.perform(
				post("/app/user/login").contentType("application/json").content(mapper.writeValueAsBytes(mapOf("id" to id, "pw" to "test-password"))),
			).andReturn().response
		assertEquals(200, login.status)
		val token = mapper.readTree(login.contentAsByteArray)["accessToken"].asText()
		val profile = mvc.perform(get("/app/user/profile").header("Authorization", "Bearer $token")).andReturn().response
		assertEquals(200, profile.status)
		assertEquals("학생", mapper.readTree(profile.contentAsByteArray)["name"].asText())
		assertEquals(401, mvc.perform(get("/app/user/profile")).andReturn().response.status)
	}
}
