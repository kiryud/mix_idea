package com.ideasupport.infra.ai

import org.springframework.stereotype.Component
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Network boundary: tests replace this component without contacting the provider. */
@Component
class GeminiHttpComponent {
	private val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build()

	fun send(request: HttpRequest): HttpResponse<String> = client.send(request, HttpResponse.BodyHandlers.ofString())
}
