package com.ideasupport.lib.common.handler
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.*
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.*
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.resource.NoResourceFoundException
import com.ideasupport.lib.common.dto.ServerErrorResponseDto
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@RestControllerAdvice
class GlobalServerErrorExceptionHandler {
	private val log = LoggerFactory.getLogger(javaClass)

	@ExceptionHandler(ServerErrorException::class)
	fun domain(ex: ServerErrorException): ResponseEntity<ServerErrorResponseDto> {
		val response = ResponseEntity.status(ex.httpStatus)
		if (ex.httpStatus == HttpStatus.TOO_MANY_REQUESTS) response.header("Retry-After", (ex.retryAfterSeconds ?: 60).toString())
		return response.body(ex.mappingServerErrorResponseDto())
	}

	@ExceptionHandler(
		MethodArgumentNotValidException::class,
		HttpMessageNotReadableException::class,
		MethodArgumentTypeMismatchException::class,
		ConstraintViolationException::class,
	)
	fun input(ex: Exception): ResponseEntity<ServerErrorResponseDto> =
		ResponseEntity.badRequest().body(
			INTERNAL_ERROR_CODE.INVALID_INPUT.mappingServerErrorResponseDto(),
		)

	@ExceptionHandler(NoResourceFoundException::class)
	fun missing(): ResponseEntity<ServerErrorResponseDto> =
		ResponseEntity.status(
			404,
		).body(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND.mappingServerErrorResponseDto())

	@ExceptionHandler(Exception::class)
	fun unexpected(
		ex: Exception,
		request: HttpServletRequest,
	): ResponseEntity<ServerErrorResponseDto> {
		// 요청 헤더·본문·비밀값을 오류 로그에 남기지 않는다.
		log.error("Unhandled API failure: {} {} ({})", request.method, request.requestURI, ex.javaClass.simpleName)
		return ResponseEntity.internalServerError().body(ServerErrorResponseDto(9997, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."))
	}
}
