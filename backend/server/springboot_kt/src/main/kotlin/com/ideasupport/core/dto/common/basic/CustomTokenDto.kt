package com.ideasupport.core.dto.common.basic

data class CustomTokenDto(
	val uid: String,
	val email: String? = null,
	val name: String? = null,
	val isEmailVerified: Boolean? = null,
	val picture: String? = null,
)
