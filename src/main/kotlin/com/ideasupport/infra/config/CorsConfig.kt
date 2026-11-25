package com.ideasupport.infra.config
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.cors.*

@Configuration
class CorsConfig(
	@Value("\${app.cors.origins:*}") private val origins: String,
) {
	@Bean fun corsConfigurationSource(): CorsConfigurationSource =
		UrlBasedCorsConfigurationSource().also { source ->
			val config = CorsConfiguration()
			val originList = origins.split(",").map { it.trim() }.filter { it.isNotBlank() }
			if (originList.contains("*") || originList.isEmpty()) {
				config.allowedOriginPatterns = listOf("*")
			} else {
				config.allowedOriginPatterns = originList + listOf("*")
			}
			config.allowedMethods = listOf("GET", "POST", "PUT", "OPTIONS", "DELETE")
			config.allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "X-Requested-With")
			config.exposedHeaders = listOf("Retry-After")
			config.allowCredentials = false
			source.registerCorsConfiguration("/**", config)
		}
}
