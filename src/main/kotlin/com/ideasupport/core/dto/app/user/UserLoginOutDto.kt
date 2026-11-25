package com.ideasupport.core.dto.app.user

data class UserLoginOutDto(
	val accessToken: String,
	val refreshToken: String,
)
