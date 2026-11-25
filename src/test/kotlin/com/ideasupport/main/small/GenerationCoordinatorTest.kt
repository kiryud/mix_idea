package com.ideasupport.main.small
import com.ideasupport.lib.common.component.GenerationCoordinatorComponent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

class GenerationCoordinatorTest {
	@Test fun `concurrent consumers share one result and failed generation can retry`() {
		val coordinator = GenerationCoordinatorComponent()
		val calls = AtomicInteger()
		val started = CountDownLatch(1)
		val release = CountDownLatch(1)
		val pool = Executors.newFixedThreadPool(6)
		try {
			val first =
				pool.submit<String> {
					coordinator.execute("same") {
						calls.incrementAndGet()
						started.countDown()
						release.await(5, TimeUnit.SECONDS)
						"result"
					}
				}
			assertTrue(started.await(5, TimeUnit.SECONDS))
			val followers =
				(1..5).map {
					pool.submit<String> {
						coordinator.execute("same") {
							calls.incrementAndGet()
							"duplicate"
						}
					}
				}
			Thread.sleep(100)
			release.countDown()
			assertEquals("result", first.get(5, TimeUnit.SECONDS))
			followers.forEach { assertEquals("result", it.get(5, TimeUnit.SECONDS)) }
			assertEquals(1, calls.get())
			assertThrows(IllegalStateException::class.java) { coordinator.execute("retry") { error("failed") } }
			assertEquals("recovered", coordinator.execute("retry") { "recovered" })
		} finally {
			release.countDown()
			pool.shutdownNow()
		}
	}
}
