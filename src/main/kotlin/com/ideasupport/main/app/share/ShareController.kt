package com.ideasupport.main.app.share
import org.springframework.web.bind.annotation.*
import org.springframework.http.*
import io.swagger.v3.oas.annotations.tags.Tag
import com.ideasupport.lib.common.annotation.Api
import com.ideasupport.lib.common.objects.ApiPaths

@Tag(name = "Share", description = "공개 읽기 전용 페이지")
@RestController
class ShareController(private val service: ShareService) {
	@Api(path = ApiPaths.PUBLIC_SHARE, summary = "공유 페이지", description = "앱과 로그인 없이 공유 아이디어를 확인합니다.")
	fun page(
		@PathVariable code: String,
	): ResponseEntity<String> =
		ResponseEntity.ok().contentType(MediaType.TEXT_HTML)
			.header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; frame-ancestors 'none'")
			.header("X-Content-Type-Options", "nosniff").header("Cache-Control", "no-store").body(service.page(code))
}
