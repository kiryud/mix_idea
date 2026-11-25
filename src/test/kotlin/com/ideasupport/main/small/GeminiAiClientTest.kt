package com.ideasupport.main.small

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.ideasupport.infra.ai.GeminiAiClient
import com.ideasupport.infra.ai.GeminiHttpComponent
import com.ideasupport.lib.common.exception.ServerErrorException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.mockito.kotlin.any
import org.mockito.Mockito.*
import java.net.http.HttpResponse
import java.net.http.HttpTimeoutException
import java.io.IOException

class GeminiAiClientTest {
	@TestFactory fun `typed guides decode through the Gemini transport without live calls`() =
		listOf("software", "contest", "general").map { kind ->
			dynamicTest(kind) {
				respond(
					body =
						envelope(
							"""{"kind":"$kind","feasible":true,"summary":"분야별 가이드","requirements":[{"name":"준비","detail":"자료"}],"steps":[{"order":1,"title":"검증","description":"작게 시작합니다"}]}""",
						),
				)
				val result = client().generateGuide("카테고리", "아이디어 제목", "구체적인 아이디어", kind)
				assertEquals(kind, result.kind)
				assertEquals("자료", result.requirements.first().detail)
			}
		}

	@Test fun `unsupported guide type never calls the provider`() {
		assertThrows(ServerErrorException::class.java) { client().generateGuide("분야", "제목", "설명", "unknown") }
		verifyNoInteractions(transport)
	}

	private val mapper = jacksonObjectMapper()
	private val transport = mock(GeminiHttpComponent::class.java)

	private fun client(
		key: String = "test-only-key",
		model: String = "test-model",
	) = GeminiAiClient(
		mapper,
		key,
		model,
		5,
		20,
		15,
		1,
		transport,
	)

	private fun respond(
		status: Int = 200,
		body: String,
	) {
		@Suppress("UNCHECKED_CAST")
		val response = mock(HttpResponse::class.java) as HttpResponse<String>
		`when`(response.statusCode()).thenReturn(status)
		`when`(response.body()).thenReturn(body)
		`when`(transport.send(any())).thenReturn(response)
	}

	private fun envelope(text: String) =
		mapper.writeValueAsString(
			mapOf("candidates" to listOf(mapOf("finishReason" to "STOP", "content" to mapOf("parts" to listOf(mapOf("text" to text)))))),
		)

	private fun generate(api: GeminiAiClient = client()) = api.generateIdea("음식", listOf("감자", "치즈", "캠핑"), "간편", null)

	@Test fun `valid structured response is decoded`() {
		respond(body = envelope("""{"title":"제목","content":"설명"}"""))
		assertEquals("제목", generate().title)
	}

	@TestFactory fun `provider HTTP errors map to stable domain errors`() =
		listOf(429 to 1107, 401 to 1109, 500 to 1109).map { (status, error) ->
			dynamicTest("HTTP $status") {
				respond(status, "private upstream error")
				assertEquals(error, assertThrows(ServerErrorException::class.java) { generate() }.errorCode)
			}
		}

	@TestFactory fun `invalid completion envelopes are rejected`() =
		listOf(
			"no candidates" to "{}",
			"non STOP" to """{"candidates":[{"finishReason":"MAX_TOKENS"}]}""",
			"blank content" to envelope(""),
			"oversized content" to envelope("a".repeat(50001)),
			"invalid inner JSON" to envelope("not-json"),
			"missing required fields" to envelope("{}"),
		).map { (name, body) ->
			dynamicTest(name) {
				respond(body = body)
				assertEquals(1108, assertThrows(ServerErrorException::class.java) { generate() }.errorCode)
			}
		}

	@Test fun `timeout is a 504 domain error and permit is released for retry`() {
		`when`(transport.send(any())).thenAnswer { throw HttpTimeoutException("timeout") }
		val api = client()
		assertEquals(1110, assertThrows(ServerErrorException::class.java) { generate(api) }.errorCode)
		respond(body = envelope("""{"title":"회복","content":"성공"}"""))
		assertEquals("회복", generate(api).title)
	}

	@Test fun `network failure hides provider details`() {
		`when`(transport.send(any())).thenAnswer { throw IOException("private upstream data") }
		val error = assertThrows(ServerErrorException::class.java) { generate() }
		assertEquals(1109, error.errorCode)
		assertFalse(error.message!!.contains("private"))
	}

	@Test fun `missing credentials and invalid model never contact provider`() {
		assertThrows(ServerErrorException::class.java) { generate(client(key = "")) }
		assertThrows(ServerErrorException::class.java) { generate(client(model = "../bad")) }
		verifyNoInteractions(transport)
	}

	@Test fun `thought parts are excluded from structured output`() {
		respond(
			body = """{"candidates":[{"finishReason":"STOP","content":{"parts":[{"thought":true,"text":"hidden reasoning"},{"text":"{\"title\":\"제목\",\"content\":\"설명\"}"}]}}]}""",
		)
		assertEquals("제목", generate().title)
	}

	@Test fun `word matching requires a textual array`() {
		respond(body = envelope("""{"wordIds":[123]}"""))
		assertEquals(1108, assertThrows(ServerErrorException::class.java) { client().selectWords("주제", emptyList()) }.errorCode)
	}

	@Test fun `word matching returns IDs for later candidate validation`() {
		respond(body = envelope("""{"wordIds":["w1","w2"]}"""))
		assertEquals(listOf("w1", "w2"), client().selectWords("주제", emptyList()))
	}
}
