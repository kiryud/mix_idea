package com.ideasupport.lib.common.dto

data class UserTokenInfoDto(
	val uid: String,
	val name: String? = null,
	val email: String? = null,
	val isEmailVerified: Boolean? = null,
	val picture: String? = null,
)
