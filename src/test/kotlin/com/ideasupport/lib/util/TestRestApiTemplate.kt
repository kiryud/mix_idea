package com.ideasupport.lib.util

import org.springframework.boot.test.web.client.*
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component

@Component
class TestRestApiTemplate {
	fun <T> get(
		path: String,
		responseType: Class<T>,
		urlVariable: Any? = null,
	): ResponseEntity<T> {
		val testRestTemplate = TestRestTemplate()
		val requestEntity = HttpEntity<Any>(HttpHeaders().apply { setBearerAuth("Token") })

		return testRestTemplate.exchange(
			path,
			HttpMethod.GET,
			requestEntity,
			responseType,
			urlVariable,
		)
	}

	fun <T> delete(
		path: String,
		responseType: Class<T>,
		urlVariable: Any? = null,
	): ResponseEntity<T> {
		val testRestTemplate = TestRestTemplate()
		val requestEntity = HttpEntity<Any>(HttpHeaders().apply { setBearerAuth("Token") })

		return testRestTemplate.exchange(
			path,
			HttpMethod.DELETE,
			requestEntity,
			responseType,
			urlVariable,
		)
	}

	fun <T, R> post(
		path: String,
		requestBody: R,
		responseType: Class<T>,
		urlVariable: Any? = null,
	): ResponseEntity<T> {
		val testRestTemplate = TestRestTemplate()
		val requestEntity = HttpEntity(requestBody, HttpHeaders().apply { setBearerAuth("Token") })

		return testRestTemplate.exchange(
			path,
			HttpMethod.POST,
			requestEntity,
			responseType,
			urlVariable,
		)
	}
}
