package com.ideasupport.main.large.common.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TestControllerSpec {
	@Autowired lateinit var mvc: MockMvc

	@Test fun `development endpoints are unavailable to students`() {
		assertEquals(403, mvc.perform(get("/common/error")).andReturn().response.status)
		assertEquals(403, mvc.perform(get("/app/dev/get-pw/test")).andReturn().response.status)
	}

	@Test fun `new app APIs require valid existing authentication`() {
		assertEquals(401, mvc.perform(get("/app/categories")).andReturn().response.status)
		assertEquals(401, mvc.perform(get("/app/categories").header("Authorization", "Bearer invalid")).andReturn().response.status)
	}
}
