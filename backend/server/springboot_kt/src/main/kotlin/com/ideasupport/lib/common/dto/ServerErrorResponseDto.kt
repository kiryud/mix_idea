package com.ideasupport.lib.common.dto

data class ServerErrorResponseDto(
	val code: Int,
	val name: String,
	val message: String,
)

data class InternalServerErrorResponseDto(
	val message: String,
)
