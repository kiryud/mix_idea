package com.ideasupport.main.common.health
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.ResponseEntity
import com.ideasupport.lib.common.annotation.Api
import com.ideasupport.lib.common.objects.ApiPaths
import com.ideasupport.core.dto.common.basic.HealthOutDto
import io.swagger.v3.oas.annotations.tags.Tag

@Tag(name = "Health", description = "서버 준비 상태")
@RestController
class HealthController(private val service: HealthService) {
	@Api(path = ApiPaths.HEALTH, summary = "상태 확인", description = "DB·AI 모드·기본 단어 준비 여부를 조회합니다. 유료 AI 호출은 하지 않습니다.")
	fun status(): ResponseEntity<HealthOutDto> = service.status().let { ResponseEntity.status(if (it.status == "ok") 200 else 503).body(it) }
}
