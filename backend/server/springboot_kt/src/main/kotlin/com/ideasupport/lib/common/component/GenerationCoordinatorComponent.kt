package com.ideasupport.lib.common.component
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionException
import java.util.concurrent.ConcurrentHashMap

/** 단일 API 인스턴스의 동일 키 작업을 공유한다. DB UNIQUE는 별도로 유지한다. */
@Component
class GenerationCoordinatorComponent {
	private val inFlight = ConcurrentHashMap<String, CompletableFuture<Any>>()

	@Suppress("UNCHECKED_CAST")
	fun <T : Any> execute(
		key: String,
		action: () -> T,
	): T {
		val mine = CompletableFuture<Any>()
		val existing = inFlight.putIfAbsent(key, mine)
		if (existing != null) {
			return try {
				existing.join() as T
			} catch (ex: CompletionException) {
				throw (ex.cause ?: ex)
			}
		}
		try {
			return action().also { mine.complete(it) }
		} catch (ex: Throwable) {
			mine.completeExceptionally(ex)
			throw ex
		} finally {
			inFlight.remove(key, mine)
		}
	}
}
