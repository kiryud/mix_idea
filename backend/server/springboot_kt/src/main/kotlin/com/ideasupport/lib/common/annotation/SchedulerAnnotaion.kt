package com.ideasupport.lib.common.annotation

import jakarta.transaction.Transactional
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import com.ideasupport.lib.common.dto.LoggerInputDto
import com.ideasupport.lib.common.util.Logger

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Transactional
@Scheduled
annotation class Scheduler(
	val cron: String = "",
	val initialDelay: Long = 0,
	val fixedDelayString: String = "",
)

@Aspect
@Component
class SchedulerAspect(
	private val logger: Logger,
) {
	@Before("@annotation(Scheduler) && execution(* *(..))")
	fun schedulerBefore(joinPoint: JoinPoint) {
		val method = joinPoint.signature.name
		logger.info(
			LoggerInputDto(
				uid = "Scheduler System",
				name = "EMPTY",
				email = "EMPTY",
				message = "$method Start ✅",
				method = method,
				path = "EMPTY",
				body = "",
				ip = "EMPTY",
				referer = "EMPTY",
			),
		)
	}

	@After("@annotation(Scheduler) && execution(* *(..))")
	fun schedulerAfter(joinPoint: JoinPoint) {
		val method = joinPoint.signature.name
		logger.info(
			LoggerInputDto(
				uid = "Scheduler System",
				name = "EMPTY",
				email = "EMPTY",
				message = "$method End ✅",
				method = method,
				path = "EMPTY",
				body = "",
				ip = "EMPTY",
				referer = "EMPTY",
			),
		)
	}
}
