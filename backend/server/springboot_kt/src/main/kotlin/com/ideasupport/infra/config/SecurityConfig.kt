package com.ideasupport.infra.config
import org.springframework.context.annotation.*
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.web.cors.CorsConfigurationSource

/** 업무 인증은 기존 @Auth/@Auths로 처리한다. 개발 도구는 학생 API에서 노출하지 않는다. */
@Configuration
@EnableWebSecurity
class SecurityConfig(private val corsConfigurationSource: CorsConfigurationSource) {
	@Bean fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http.csrf { it.disable() }.cors { it.configurationSource(corsConfigurationSource) }
			.sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
			.authorizeHttpRequests {
				it.requestMatchers("/app/dev/**", "/common/ip/**", "/common/error", "/common/user-level-lock").denyAll().anyRequest().permitAll()
			}
		return http.build()
	}
}
