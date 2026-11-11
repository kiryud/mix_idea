package com.ideasupport.core.dto.app.user

import jakarta.validation.constraints.*

data class UserLoginInDto(
	@field:NotBlank @field:Size(max = 100) val id: String,
	@field:NotBlank @field:Size(max = 200) val pw: String,
)
