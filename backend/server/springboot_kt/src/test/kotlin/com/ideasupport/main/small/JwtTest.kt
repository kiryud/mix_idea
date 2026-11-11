package com.ideasupport.main.small
import com.ideasupport.lib.common.util.Jwt
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.core.dto.common.user.UserCommonDto
import com.ideasupport.core.entity.UserEntity
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mockito.*

class JwtTest {
	private val users = mock(UserRepository::class.java)
	private val jwt = Jwt("aWRlYS1zdXBwb3J0LXRlc3Qtc2VjcmV0LWtleS0wMDAx", 600, 604800, users)
	private val user = UserCommonDto(uid = "member-1", email = "student@example.com", name = "학생")

	@Test fun `tokens enforce signature expiry purpose and membership`() {
		val access = jwt.createAccessToken(user)
		val refresh = jwt.createRefreshToken(user)
		assertEquals(user.uid, jwt.validateToken(access).uid)
		assertThrows(ServerErrorException::class.java) { jwt.validateToken(refresh) }
		assertThrows(ServerErrorException::class.java) { jwt.refreshToken(access) }
		assertThrows(ServerErrorException::class.java) { jwt.refreshToken(jwt.createRefreshToken(user, -60)) }
		assertThrows(ServerErrorException::class.java) { jwt.validateToken(access + "tamper") }
		assertThrows(ServerErrorException::class.java) { jwt.refreshToken(refresh) }
		`when`(users.findByUid("member-1")).thenReturn(UserEntity(uid = "member-1", email = user.email, name = user.name))
		assertEquals(user.uid, jwt.validateToken(jwt.refreshToken(refresh)).uid)
	}
}
