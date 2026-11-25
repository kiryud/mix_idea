package com.ideasupport.lib.common.interceptor

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor
import com.ideasupport.lib.common.component.AuthComponent
import com.ideasupport.lib.common.dto.LoggerInputDto
import com.ideasupport.lib.common.dto.UserTokenInfoDto
import com.ideasupport.lib.common.util.Etc
import com.ideasupport.lib.common.util.Logger

@Component
class LogInterceptor
	@Autowired
	constructor(
		private val logger: Logger,
		private val authComponent: AuthComponent,
		private val etc: Etc,
	) : HandlerInterceptor {
		/**
		 * [LogInterceptor]
		 * - 로그 인터셉터
		 * - API 호출 성공 시 로그 남김
		 */
		override fun afterCompletion(
			request: HttpServletRequest,
			response: HttpServletResponse,
			handler: Any,
			ex: Exception?,
		) {
			if (response.status == 200) {
				var userTokenInfoDto: UserTokenInfoDto? = null
				kotlin
					.runCatching {
						authComponent.getUserTokenInfo()
					}.onSuccess {
						userTokenInfoDto = it
					}

				if (
					request.requestURI.startsWith("/app") ||
					request.requestURI.startsWith("/admin") ||
					request.requestURI.startsWith("/common")
				) {
					logger.info(
						LoggerInputDto(
							uid = userTokenInfoDto?.uid ?: "Unknown",
							name = userTokenInfoDto?.name ?: "Unknown",
							email = userTokenInfoDto?.email ?: "Unknown",
							message = "[${request.method}] ${request.requestURI} OK ✅",
							method = request.method,
							path = etc.getRequestPath(request),
							body = "[body omitted]",
							ip = etc.getIp(request),
							referer = request.getHeader("Page") ?: request.getHeader("Referer"),
						),
					)
				}
			}
		}
	}
