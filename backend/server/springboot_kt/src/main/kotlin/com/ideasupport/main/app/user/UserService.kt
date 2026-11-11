package com.ideasupport.main.app.user
import org.springframework.stereotype.Service
import org.springframework.dao.DataIntegrityViolationException
import com.ideasupport.core.dto.app.user.*
import com.ideasupport.core.dto.common.user.UserCommonDto
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Service
class UserService(private val user: UserComponent, private val identity: UserIdentityComponent) {
	fun getProfile(): UserCommonDto = identity.current().mappingUserCommonDto()

	fun signUp(input: UserSignUpInDto): UserCommonDto =
		try {
			user.signUp(input)
		} catch (ex: DataIntegrityViolationException) {
			throw ServerErrorException(INTERNAL_ERROR_CODE.ALREADY_EXIST_USER)
		}

	fun login(input: UserLoginInDto) = user.login(input)

	fun refreshToken(input: UserRefreshTokenInDto) = user.refresh(input)
}
