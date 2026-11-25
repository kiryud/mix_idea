package com.ideasupport.main.app.guide

import com.ideasupport.core.dto.app.guide.CreateGuideOutDto
import com.ideasupport.lib.common.annotation.Api
import com.ideasupport.lib.common.annotation.Auths
import com.ideasupport.lib.common.objects.ApiPaths
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Guide", description = "분야별 아이디어 후속 가이드")
@Auths
@RestController
class GuideController(private val service: GuideService) {
	@Api(
		path = ApiPaths.IDEA_GUIDE,
		method = RequestMethod.POST,
		summary = "분야별 후속 가이드",
		description = "카테고리에 맞는 조리법·구현 가이드·공모전 기획안·실행 계획을 생성하고 재사용합니다.",
	)
	fun create(
		@PathVariable id: String,
	): ResponseEntity<CreateGuideOutDto> = service.create(id).let { ResponseEntity.status(if (it.cached) 200 else 201).body(it) }
}
