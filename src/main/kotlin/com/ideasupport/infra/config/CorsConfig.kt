package com.ideasupport.infra.config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.cors.*

@Configuration
class CorsConfig(
	@Value("\${app.cors.origins}") private val origins: String,
) {
	@Bean fun corsConfigurationSource(): CorsConfigurationSource =
		UrlBasedCorsConfigurationSource().also { source ->
			val config = CorsConfiguration()
			val list = origins.split(",").map { it.trim() }.filter { it.isNotBlank() }
			config.allowedOriginPatterns = list + listOf("http://localhost:*", "http://127.0.0.1:*")
			config.allowedMethods = listOf("GET", "POST", "PUT", "OPTIONS")
			config.allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
			config.exposedHeaders = listOf("Retry-After")
			config.allowCredentials = false
			source.registerCorsConfiguration("/**", config)
		}
}
