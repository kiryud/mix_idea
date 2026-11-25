package com.ideasupport.infra.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import com.ideasupport.lib.common.component.AuthComponent
import com.ideasupport.lib.common.interceptor.LogInterceptor
import com.ideasupport.lib.common.util.Etc
import com.ideasupport.lib.common.util.Logger

@Configuration
class WebConfig
	@Autowired
	constructor(
		private val logger: Logger,
		private val authComponent: AuthComponent,
		private val etc: Etc,
	) : WebMvcConfigurer {
		override fun addInterceptors(registry: InterceptorRegistry) {
			// path need to log check
			val logCheckList = listOf("/**")
			// path don`t need to log check
			val logExceptionCheckList = listOf("/icons/**", "/css/**", "/js/**", "/plugins/**", "/images/**")

			registry.addInterceptor(LogInterceptor(logger, authComponent, etc))
				.addPathPatterns(logCheckList)
				.excludePathPatterns(logExceptionCheckList)
		}
	}
