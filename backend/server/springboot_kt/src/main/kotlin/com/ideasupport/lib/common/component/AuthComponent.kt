package com.ideasupport.lib.common.component
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import com.ideasupport.core.entity.UserRole
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.lib.common.dto.UserTokenInfoDto
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.lib.common.util.Jwt

@Component
class AuthComponent(private val jwt: Jwt, private val users: UserRepository) {
	fun getUserTokenInfo(role: UserRole? = UserRole.USER): UserTokenInfoDto {
		val request = (RequestContextHolder.currentRequestAttributes() as ServletRequestAttributes).request
		val authorization = request.getHeader("Authorization") ?: throw ServerErrorException(INTERNAL_ERROR_CODE.AUTHORIZATION_NOT_FOUND)
		if (!authorization.startsWith("Bearer ", ignoreCase = true) || authorization.length <= 7) {
			throw ServerErrorException(
				INTERNAL_ERROR_CODE.INVALID_TOKEN,
			)
		}
		val token = jwt.validateToken(authorization.substring(7))
		val user = users.findByUid(token.uid) ?: throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_TOKEN)
		if (role == UserRole.ADMIN && user.role != UserRole.ADMIN) throw ServerErrorException(INTERNAL_ERROR_CODE.NOT_ADMIN)
		return UserTokenInfoDto(uid = token.uid, email = user.email, name = user.name)
	}
}
