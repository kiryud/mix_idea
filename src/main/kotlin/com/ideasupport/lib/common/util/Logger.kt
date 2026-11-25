package com.ideasupport.lib.common.util

import mu.KLogger
import mu.KotlinLogging
import mu.withLoggingContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.ideasupport.lib.common.dto.LoggerInputDto
import com.ideasupport.lib.common.objects.Profiles

@Component
class Logger(
	private val slackWebhook: SlackWebhook,
	@Autowired private val etc: Etc,
) {
	private val logger: KLogger = KotlinLogging.logger {}

	/**
	 * [Logger]
	 * - logger trace
	 * */
	fun trace(loggerInputDto: LoggerInputDto) {
		return withLoggingContext(
			mapOf(
				"uid" to loggerInputDto.uid,
				"name" to loggerInputDto.name,
				"email" to loggerInputDto.email,
				"fileName" to loggerInputDto.fileName,
				"funcName" to loggerInputDto.funcName,
				"code" to loggerInputDto.code.toString(),
				"method" to loggerInputDto.method,
				"path" to loggerInputDto.path,
			),
		) {
			logger.trace { loggerInputDto.message }
		}
	}

	/**
	 * [Logger]
	 * - logger debug
	 * */
	fun debug(loggerInputDto: LoggerInputDto) {
		return withLoggingContext(
			mapOf(
				"uid" to loggerInputDto.uid,
				"name" to loggerInputDto.name,
				"email" to loggerInputDto.email,
				"fileName" to loggerInputDto.fileName,
				"funcName" to loggerInputDto.funcName,
				"code" to loggerInputDto.code.toString(),
				"method" to loggerInputDto.method,
				"path" to loggerInputDto.path,
			),
		) {
			logger.debug { loggerInputDto.message }
		}
	}

	/**
	 * [Logger]
	 * - logger info
	 * */
	fun info(loggerInputDto: LoggerInputDto) {
		return withLoggingContext(
			mapOf(
				"uid" to loggerInputDto.uid,
				"name" to loggerInputDto.name,
				"email" to loggerInputDto.email,
				"method" to loggerInputDto.method,
				"path" to loggerInputDto.path,
				"body" to loggerInputDto.body,
				"ip" to loggerInputDto.ip,
				"referer" to loggerInputDto.referer,
			),
		) {
			logger.info { loggerInputDto.message }
		}
	}

	/**
	 * [Logger]
	 * - logger warn
	 * */
	fun warn(loggerInputDto: LoggerInputDto) {
		return withLoggingContext(
			mapOf(
				"uid" to loggerInputDto.uid,
				"name" to loggerInputDto.name,
				"email" to loggerInputDto.email,
				"fileName" to loggerInputDto.fileName,
				"funcName" to loggerInputDto.funcName,
				"code" to loggerInputDto.code.toString(),
				"method" to loggerInputDto.method,
				"path" to loggerInputDto.path,
				"body" to loggerInputDto.body,
				"ip" to loggerInputDto.ip,
				"referer" to loggerInputDto.referer,
			),
		) {
			logger.warn { loggerInputDto.message + " 🟡" }
			if (etc.getActiveProfile() != Profiles.PROD && etc.getActiveProfile() != Profiles.TEST) slackWebhook.warn(loggerInputDto)
		}
	}

	/**
	 * [Logger]
	 * - logger error
	 *
	 * */
	fun error(loggerInputDto: LoggerInputDto) {
		return withLoggingContext(
			mapOf(
				"uid" to loggerInputDto.uid,
				"name" to loggerInputDto.name,
				"email" to loggerInputDto.email,
				"fileName" to loggerInputDto.fileName,
				"funcName" to loggerInputDto.funcName,
				"code" to loggerInputDto.code.toString(),
				"method" to loggerInputDto.method,
				"path" to loggerInputDto.path,
				"body" to loggerInputDto.body,
				"ip" to loggerInputDto.ip,
				"referer" to loggerInputDto.referer,
			),
		) {
			logger.error { loggerInputDto.message + " ❌" }
			if (etc.getActiveProfile() != Profiles.TEST) slackWebhook.error(loggerInputDto)
		}
	}
}
