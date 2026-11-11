package com.ideasupport.lib.common.util
import io.jsonwebtoken.*
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Date
import java.time.Instant
import com.ideasupport.core.dto.common.basic.CustomTokenDto
import com.ideasupport.core.dto.common.user.UserCommonDto
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

/** 기존 자체 JWT의 발급·갱신 계약을 유지한다. 만료 및 토큰 용도는 엄격히 검증한다. */
@Component
class Jwt(
	@Value("\${jwt.secret}") secretKey: String,
	@Value("\${jwt.access_token_expiration_time:600}") private val accessTokenExpTime: Long,
	@Value("\${jwt.refresh_token_expiration_time:604800}") private val refreshTokenExpTime: Long,
	private val userRepository: UserRepository,
) {
	private val key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey))

	fun createAccessToken(user: UserCommonDto): String = create(user, accessTokenExpTime, "access")

	fun createRefreshToken(user: UserCommonDto): String = create(user, refreshTokenExpTime, "refresh")

	fun createRefreshToken(
		user: UserCommonDto,
		expireTime: Long,
	): String = create(user, expireTime, "refresh")

	private fun create(
		user: UserCommonDto,
		seconds: Long,
		type: String,
	): String =
		Jwts.builder()
			.setIssuer(
				"idea-support-server",
			).setSubject(user.uid).claim("uid", user.uid).claim("email", user.email).claim("name", user.name).claim("tokenType", type)
			.setIssuedAt(Date()).setExpiration(Date.from(Instant.now().plusSeconds(seconds))).signWith(key, SignatureAlgorithm.HS256).compact()

	fun parseClaims(token: String?): Claims =
		try {
			Jwts.parserBuilder().requireIssuer("idea-support-server").setSigningKey(key).build().parseClaimsJws(token).body
		} catch (ex: Exception) {
			throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_TOKEN)
		}

	private fun claims(
		token: String?,
		type: String,
	): Claims =
		parseClaims(token).also {
			if (it["tokenType"] != type || it["uid"] !is String) throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_TOKEN)
		}

	fun validateToken(token: String?): CustomTokenDto {
		val data = claims(token, "access")
		return CustomTokenDto(uid = data["uid", String::class.java], email = data["email"] as? String, name = data["name"] as? String)
	}

	fun refreshToken(refreshToken: String): String {
		val data = claims(refreshToken, "refresh")
		val user = userRepository.findByUid(data["uid", String::class.java]) ?: throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_TOKEN)
		return createAccessToken(user.mappingUserCommonDto())
	}

	fun getUserId(token: String?): Long =
		userRepository.findByUid(validateToken(token).uid)?.idx ?: throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_TOKEN)
}
