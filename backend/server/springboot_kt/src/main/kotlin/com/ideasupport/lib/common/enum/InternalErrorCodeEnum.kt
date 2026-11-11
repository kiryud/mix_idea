package com.ideasupport.lib.common.enum

import ch.qos.logback.core.status.ErrorStatus
import org.springframework.http.HttpStatus
import com.ideasupport.lib.common.dto.ServerErrorResponseDto

@Suppress("ktlint:standard:class-naming")
enum class INTERNAL_ERROR_CODE(val code: Int, val message: String, val httpStatus: HttpStatus, val errorStatus: Int) {
	// Auth : 1 ~ 50
	AUTHORIZATION_NOT_FOUND(1, "Authorization 헤더가 존재하지 않음", HttpStatus.UNAUTHORIZED, ErrorStatus.WARN),
	INVALID_TOKEN(2, "유효하지 않은 토큰", HttpStatus.UNAUTHORIZED, ErrorStatus.WARN),
	NOT_ADMIN(3, "관리자 권한이 없음", HttpStatus.FORBIDDEN, ErrorStatus.WARN),
	NOT_ALLOW_IP(4, "허용되지 않은 IP", HttpStatus.FORBIDDEN, ErrorStatus.WARN),
	DENY_IP(5, "차단된 IP", HttpStatus.FORBIDDEN, ErrorStatus.WARN),
	WRONG_PW(6, "비밀번호가 틀림", HttpStatus.BAD_REQUEST, ErrorStatus.WARN),

	// User : 51 ~ 100
	ALREADY_EXIST_USER(51, "이미 존재하는 유저", HttpStatus.CONFLICT, ErrorStatus.WARN),
	ALREADY_EXIST_NICKNAME(52, "이미 존재하는 닉네임", HttpStatus.CONFLICT, ErrorStatus.WARN),
	NOT_FOUND_USER(53, "존재하지 않는 유저", HttpStatus.NOT_FOUND, ErrorStatus.WARN),

	// Idea Support
	INVALID_INPUT(1100, "입력값을 확인해 주세요.", HttpStatus.BAD_REQUEST, ErrorStatus.WARN),
	OWN_POST_REACTION_FORBIDDEN(1101, "내 글에는 반응할 수 없습니다.", HttpStatus.FORBIDDEN, ErrorStatus.WARN),
	RESOURCE_NOT_FOUND(1102, "요청한 데이터를 찾을 수 없습니다.", HttpStatus.NOT_FOUND, ErrorStatus.WARN),
	DIRECTION_TOO_NARROW(1103, "연관 단어가 부족합니다. 방향을 넓혀 주세요.", HttpStatus.UNPROCESSABLE_ENTITY, ErrorStatus.WARN),
	WORDS_INSUFFICIENT(1104, "카테고리의 단어가 부족합니다.", HttpStatus.UNPROCESSABLE_ENTITY, ErrorStatus.WARN),
	CATEGORY_MATCH_INSUFFICIENT(1105, "카테고리에 맞는 단어가 10개 미만입니다.", HttpStatus.UNPROCESSABLE_ENTITY, ErrorStatus.WARN),
	UNSAFE_CONTENT(1106, "허용되지 않는 내용입니다.", HttpStatus.UNPROCESSABLE_ENTITY, ErrorStatus.WARN),
	RATE_LIMITED(1107, "요청이 많습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.TOO_MANY_REQUESTS, ErrorStatus.WARN),
	AI_RESPONSE_INVALID(1108, "AI 결과 형식이 올바르지 않습니다.", HttpStatus.BAD_GATEWAY, ErrorStatus.WARN),
	AI_UNAVAILABLE(1109, "AI 서비스에 연결할 수 없습니다.", HttpStatus.SERVICE_UNAVAILABLE, ErrorStatus.WARN),
	AI_TIMEOUT(1110, "AI 응답 시간이 초과되었습니다.", HttpStatus.GATEWAY_TIMEOUT, ErrorStatus.WARN),
	RECIPE_NOT_SUPPORTED(1111, "만드는 법은 음식 개발 카테고리에서만 제공합니다.", HttpStatus.UNPROCESSABLE_ENTITY, ErrorStatus.WARN),

	// Database
	NOT_FOUND_REPO(9996, "존재하지 않는 Repository", HttpStatus.NOT_FOUND, ErrorStatus.WARN),
	NOT_FOUND_DATA(9998, "데이터가 존재하지 않음", HttpStatus.NOT_FOUND, ErrorStatus.WARN),
	DB_USER_LEVEL_LOCK_NOT_RELEASE(9999, "해당 유저 DB Lock", HttpStatus.LOCKED, ErrorStatus.ERROR),

	// Test
	TEST(10000, "테스트 에러", HttpStatus.INTERNAL_SERVER_ERROR, ErrorStatus.ERROR),
	;

	fun mappingServerErrorResponseDto(): ServerErrorResponseDto {
		return ServerErrorResponseDto(
			code = code,
			name = name,
			message = message,
		)
	}
}
