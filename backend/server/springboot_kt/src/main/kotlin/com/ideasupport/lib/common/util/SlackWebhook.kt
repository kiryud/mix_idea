package com.ideasupport.lib.common.util

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.Instant
import java.time.ZoneId
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import com.ideasupport.lib.common.dto.LoggerInputDto
import com.ideasupport.lib.common.objects.Util

@Component
class SlackWebhook {
	@Value("\${logging.slack.webhook.url:}")
	lateinit var slackWebhookURL: String

	fun error(loggerInputDto: LoggerInputDto) {
		if (slackWebhookURL.isBlank()) return
		val utc = Instant.now()
		val kst = utc.plusSeconds(32400)
		val utcDateTime = utc.atZone(ZoneId.systemDefault())
		val kstDateTime = kst.atZone(ZoneId.systemDefault())
		val truncatedBody =
			loggerInputDto.body?.let { body ->
				if (body.length > Util.SLACK_WEBHOOK_BODY_MAX_LENGTH) {
					body.substring(0, Util.SLACK_WEBHOOK_BODY_MAX_LENGTH_WITH_ELLIPSIS) + "\n\n ... 생략"
				} else {
					body
				}
			}
		val payload =
			setPayload(
				header = "🚨   에러가 발생하였습니다.   🚨",
				utc = "${utcDateTime.year}년 ${utcDateTime.monthValue}월 ${utcDateTime.dayOfMonth}일 ${utcDateTime.hour}시 ${utcDateTime.minute}분 ${utcDateTime.second}초",
				kst = "${kstDateTime.year}년 ${kstDateTime.monthValue}월 ${kstDateTime.dayOfMonth}일 ${kstDateTime.hour}시 ${kstDateTime.minute}분 ${kstDateTime.second}초",
				id = loggerInputDto.uid,
				method = loggerInputDto.method,
				path = loggerInputDto.path,
				name = loggerInputDto.name,
				email = loggerInputDto.email,
				fileName = loggerInputDto.fileName,
				funcName = loggerInputDto.funcName,
				code = loggerInputDto.code,
				errorName = loggerInputDto.errorName,
				errorMessage = loggerInputDto.errorMessage,
				body = truncatedBody.takeIf { it!!.isNotBlank() } ?: "EMPTY",
				ip = loggerInputDto.ip,
				referer = loggerInputDto.referer,
				headers = loggerInputDto.headers,
			)

		val restTemplate = RestTemplate()
		val headers = HttpHeaders()
		headers.contentType = MediaType.APPLICATION_JSON

		val entity = HttpEntity(ObjectMapper().writeValueAsString(payload), headers)
		restTemplate.exchange(slackWebhookURL, HttpMethod.POST, entity, String::class.java)
	}

	fun warn(loggerInputDto: LoggerInputDto) {
		if (slackWebhookURL.isBlank()) return
		val utc = Instant.now()
		val kst = utc.plusSeconds(32400)
		val utcDateTime = utc.atZone(ZoneId.systemDefault())
		val kstDateTime = kst.atZone(ZoneId.systemDefault())
		val truncatedBody =
			loggerInputDto.body?.let { body ->
				if (body.length > Util.SLACK_WEBHOOK_BODY_MAX_LENGTH) {
					body.substring(0, Util.SLACK_WEBHOOK_BODY_MAX_LENGTH_WITH_ELLIPSIS) + "\n\n ... 생략"
				} else {
					body
				}
			}
		val payload =
			setPayload(
				header = "🟡   경고가 발생하였습니다.   🟡",
				utc = "${utcDateTime.year}년 ${utcDateTime.monthValue}월 ${utcDateTime.dayOfMonth}일 ${utcDateTime.hour}시 ${utcDateTime.minute}분 ${utcDateTime.second}초",
				kst = "${kstDateTime.year}년 ${kstDateTime.monthValue}월 ${kstDateTime.dayOfMonth}일 ${kstDateTime.hour}시 ${kstDateTime.minute}분 ${kstDateTime.second}초",
				id = loggerInputDto.uid,
				method = loggerInputDto.method,
				path = loggerInputDto.path,
				name = loggerInputDto.name,
				email = loggerInputDto.email,
				fileName = loggerInputDto.fileName,
				funcName = loggerInputDto.funcName,
				code = loggerInputDto.code,
				errorName = loggerInputDto.errorName,
				errorMessage = loggerInputDto.errorMessage,
				body = truncatedBody.takeIf { it!!.isNotBlank() } ?: "EMPTY",
				ip = loggerInputDto.ip,
				referer = loggerInputDto.referer,
				headers = loggerInputDto.headers,
			)

		val restTemplate = RestTemplate()
		val headers = HttpHeaders()
		headers.contentType = MediaType.APPLICATION_JSON

		val entity = HttpEntity(ObjectMapper().writeValueAsString(payload), headers)
		restTemplate.exchange(slackWebhookURL, HttpMethod.POST, entity, String::class.java)
	}

	private fun setPayload(
		header: String,
		utc: String,
		kst: String,
		id: String,
		method: String?,
		path: String?,
		name: String,
		email: String,
		fileName: String?,
		funcName: String?,
		code: Int?,
		errorName: String?,
		errorMessage: String?,
		body: String?,
		ip: String?,
		referer: String?,
		headers: Map<String, String>? = null,
	): Map<String, List<Map<String, Any>>> {
		val payload =
			mapOf(
				"blocks" to
					listOf(
						mapOf(
							"type" to "header",
							"text" to
								mapOf(
									"type" to "plain_text",
									"text" to header,
									"emoji" to true,
								),
						),
						mapOf(
							"type" to "section",
							"text" to
								mapOf(
									"type" to "mrkdwn",
									"text" to "@channel",
								),
						),
						mapOf(
							"type" to "section",
							"fields" to
								listOf(
									mapOf(
										"type" to "mrkdwn",
										"text" to "*날짜* \n $utc(UTC) \n $kst(KST)",
									),
									mapOf(
										"type" to "mrkdwn",
										"text" to "*아이디* \n $id",
									),
								),
						),
						mapOf(
							"type" to "section",
							"fields" to
								listOf(
									mapOf(
										"type" to "mrkdwn",
										"text" to "*메소드* \n $method",
									),
									mapOf(
										"type" to "mrkdwn",
										"text" to "*패스* \n $path",
									),
								),
						),
						mapOf(
							"type" to "section",
							"fields" to
								listOf(
									mapOf(
										"type" to "mrkdwn",
										"text" to "*닉네임* \n $name",
									),
									mapOf(
										"type" to "mrkdwn",
										"text" to "*이메일* \n $email",
									),
								),
						),
						mapOf(
							"type" to "section",
							"fields" to
								listOf(
									mapOf(
										"type" to "mrkdwn",
										"text" to "*파일 이름* \n $fileName",
									),
									mapOf(
										"type" to "mrkdwn",
										"text" to "*함수 이름* \n $funcName",
									),
								),
						),
						mapOf(
							"type" to "section",
							"fields" to
								listOf(
									mapOf(
										"type" to "mrkdwn",
										"text" to "*에러 이름* \n $errorName",
									),
									mapOf(
										"type" to "mrkdwn",
										"text" to "*에러 설명* \n $errorMessage",
									),
								),
						),
						mapOf(
							"type" to "section",
							"fields" to
								listOf(
									mapOf(
										"type" to "mrkdwn",
										"text" to "*코드* \n $code",
									),
									mapOf(
										"type" to "mrkdwn",
										"text" to "*아이피* \n $ip",
									),
								),
						),
						mapOf(
							"type" to "section",
							"fields" to
								listOf(
									mapOf(
										"type" to "mrkdwn",
										"text" to "*요청 페이지* \n $referer",
									),
								),
						),
						/*mapOf(
							"type" to "section",
							"text" to
								mapOf(
									"type" to "mrkdwn",
									"text" to "*헤더* \n ```\n\n${headers}\n\n```",
								),
						),*/
						mapOf(
							"type" to "section",
							"text" to
								mapOf(
									"type" to "mrkdwn",
									"text" to "*요청 데이터* \n ```\n\n${body}\n\n```",
								),
						),
						mapOf(
							"type" to "divider",
						),
					),
			)

		return payload
	}
}
