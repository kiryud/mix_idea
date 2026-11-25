package com.ideasupport.lib.common.component
import org.springframework.stereotype.Component
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Instant
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class RequestLimitComponent(
	@Value("\${app.limits.generation:10}") private val generationLimit: Int,
	@Value("\${app.limits.recipe:5}") private val recipeLimit: Int,
	@Value("\${app.limits.preview:20}") private val previewLimit: Int,
	@Value("\${app.limits.category:10}") private val categoryLimit: Int,
) {
	private data class Window(val expires: Long, var count: Int)

	private val windows = mutableMapOf<String, Window>()

	fun check(
		action: String,
		userIdx: Long,
	) {
		val limit =
			when (action) {
				"generation" -> generationLimit
				"recipe" -> recipeLimit
				"category" -> categoryLimit
				else -> previewLimit
			}
		val duration = if (action == "category") 86400L else 60L
		val ip = (RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes)?.request?.remoteAddr ?: "background"
		// 프록시 헤더는 신뢰 프록시를 설정하기 전까지 사용하지 않는다.
		synchronized(windows) {
			val now = Instant.now().epochSecond
			windows.entries.removeIf { it.value.expires <= now }
			if (windows.size > 10000) throw ServerErrorException(INTERNAL_ERROR_CODE.RATE_LIMITED)
			val keys = listOf("$action:user:$userIdx" to limit, "$action:ip:$ip" to limit * 3)
			val exceeded = keys.filter { (key, max) -> (windows[key]?.count ?: 0) >= max }
			if (exceeded.isNotEmpty()) {
				throw ServerErrorException(
					INTERNAL_ERROR_CODE.RATE_LIMITED,
					exceeded.maxOf {
							(key, _) ->
						windows.getValue(key).expires - now
					},
				)
			}
			keys.forEach { (key, _) -> windows.getOrPut(key) { Window(now + duration, 0) }.count++ }
		}
	}
}
