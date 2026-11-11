package com.ideasupport.main.app.dev

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.context.annotation.Profile
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import com.ideasupport.lib.common.annotation.Api
import com.ideasupport.lib.common.annotation.Auth
import com.ideasupport.lib.common.objects.ApiPaths
import com.ideasupport.lib.common.objects.Profiles
import com.ideasupport.lib.common.util.SHAType
import com.ideasupport.lib.common.util.Sha

@Profile(Profiles.NOT_PROD)
@Tag(name = "Dev", description = "개발")
@RestController
class DevController(
	private val sha: Sha,
) {
	@Auth
	@Api(
		path = ApiPaths.GET_PW,
		method = RequestMethod.GET,
		summary = "비밀번호 조회",
		description = "비밀번호를 조회합니다.",
	)
	fun getPW(
		@PathVariable pw: String,
	): ResponseEntity<String> = ResponseEntity.ok().body(sha.encrypt(pw, SHAType.SHA256))
}
