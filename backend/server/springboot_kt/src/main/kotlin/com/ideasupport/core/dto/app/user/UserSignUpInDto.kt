package com.ideasupport.core.dto.app.user

import jakarta.validation.constraints.*

data class UserSignUpInDto(
	@field:NotBlank @field:Size(max = 100) val id: String,
	@field:NotBlank @field:Size(max = 200) val pw: String,
	@field:Email @field:NotBlank @field:Size(max = 200) val email: String,
	@field:NotBlank @field:Size(max = 50) val name: String,
	val nickName: String? = null,
	val phoneNumber: String? = null,
	val providerName: String? = null,
	@field:NotBlank @field:Size(max = 10) val birth: String,
	val termsOfService: Boolean? = false,
	val privacyPolicy: Boolean? = false,
	val alertPolicy: Boolean? = false,
	val referralCodeReceived: String? = null,
)
