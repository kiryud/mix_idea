package com.ideasupport.lib.common.exception

import com.ideasupport.lib.common.dto.ServerErrorResponseDto
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE

class ServerErrorException(internalErrorCode: INTERNAL_ERROR_CODE, val retryAfterSeconds: Long? = null) : RuntimeException(
	internalErrorCode.message,
) {
	val errorCode = internalErrorCode.code
	val errorName = internalErrorCode.name
	val errorMessage = internalErrorCode.message
	val httpStatus = internalErrorCode.httpStatus
	val errorStatus = internalErrorCode.errorStatus

	fun mappingServerErrorResponseDto(): ServerErrorResponseDto {
		return ServerErrorResponseDto(
			code = errorCode,
			name = errorName,
			message = errorMessage,
		)
	}
}
