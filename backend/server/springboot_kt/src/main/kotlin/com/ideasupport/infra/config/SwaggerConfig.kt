package com.ideasupport.infra.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
	@Value("\${swagger.url}")
	private val swaggerURL: String? = null

	@Value("\${swagger.description}")
	private val swaggerDescription: String? = null

	@Bean
	fun customOpenAPI(): OpenAPI {
		return OpenAPI()
			.info(Info().title("Idea Support API").version("v1.0"))
			.servers(
				listOf(
					Server().url(swaggerURL)
						.description(swaggerDescription),
				),
			)
			.components(
				Components().securitySchemes(
					mapOf(
						"JWT" to
							SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT"),
					),
				),
			)
	}

	@Bean
	fun admin(): GroupedOpenApi {
		val pathsToMatch =
			arrayOf(
				"/admin/**",
			)

		return GroupedOpenApi.builder()
			.group("admin")
			.pathsToMatch(*pathsToMatch)
			.build()
	}

	@Bean
	fun app(): GroupedOpenApi {
		val pathsToMatch =
			arrayOf(
				"/app/**",
			)

		return GroupedOpenApi.builder()
			.group("app")
			.pathsToExclude("/app/dev/**")
			.pathsToMatch(*pathsToMatch)
			.build()
	}

	@Bean
	fun common(): GroupedOpenApi {
		val pathsToMatch =
			arrayOf(
				"/common/**",
			)

		return GroupedOpenApi.builder()
			.group("common")
			.pathsToExclude("/common/error", "/common/user-level-lock", "/common/ip/**")
			.pathsToMatch(*pathsToMatch)
			.build()
	}

	@Bean
	fun shared(): GroupedOpenApi = GroupedOpenApi.builder().group("share").pathsToMatch("/s/**").build()
}
