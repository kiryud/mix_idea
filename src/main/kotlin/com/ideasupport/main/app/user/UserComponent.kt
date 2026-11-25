package com.ideasupport.main.app.user
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID
import com.ideasupport.core.entity.UserEntity
import com.ideasupport.core.dto.app.user.*
import com.ideasupport.core.dto.common.user.UserCommonDto
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.lib.common.util.*
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class UserComponent(private val users: UserRepository, private val sha: Sha, private val jwt: Jwt) {
	@Transactional fun signUp(input: UserSignUpInDto): UserCommonDto {
		if (users.findById(input.id) != null) throw ServerErrorException(INTERNAL_ERROR_CODE.ALREADY_EXIST_USER)
		return users.saveAndFlush(
			UserEntity(
				uid = UUID.randomUUID().toString(),
				id = input.id,
				pw = sha.encrypt(input.pw, SHAType.SHA256),
				email = input.email,
				name = input.name,
				createdAt = Instant.now(),
			),
		).mappingUserCommonDto()
	}

	fun login(input: UserLoginInDto): UserLoginOutDto {
		val user = users.findById(input.id) ?: throw ServerErrorException(INTERNAL_ERROR_CODE.NOT_FOUND_USER)
		if (user.pw != sha.encrypt(input.pw, SHAType.SHA256)) throw ServerErrorException(INTERNAL_ERROR_CODE.WRONG_PW)
		val dto = user.mappingUserCommonDto()
		return UserLoginOutDto(jwt.createAccessToken(dto), jwt.createRefreshToken(dto))
	}

	fun refresh(input: UserRefreshTokenInDto) = UserRefreshTokenOutDto(jwt.refreshToken(input.refreshToken))
}
