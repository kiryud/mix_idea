package com.ideasupport.main.common.ip

import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RestController
import com.ideasupport.core.entity.UserRole
import com.ideasupport.lib.common.annotation.Api
import com.ideasupport.lib.common.annotation.Auths
import com.ideasupport.lib.common.objects.ApiPaths

@Auths(role = UserRole.ADMIN)
@Tag(name = "Ip", description = "관리자 ip")
@RestController
class IpController(
	private val ipService: IpService,
) {
	@Api(
		path = ApiPaths.INIT_IP,
		method = RequestMethod.GET,
		summary = "IP 초기화",
		description = "IP 초기화",
	)
	fun initIp(): ResponseEntity<Nothing> {
		ipService.initIp()

		return ResponseEntity.ok().body(null)
	}
}
