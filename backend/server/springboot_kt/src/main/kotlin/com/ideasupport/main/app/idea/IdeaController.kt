package com.ideasupport.main.app.idea
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.tags.Tag
import com.ideasupport.lib.common.annotation.*
import com.ideasupport.lib.common.objects.ApiPaths
import com.ideasupport.core.dto.app.idea.*

@Tag(name = "Idea", description = "아이디어 생성과 재사용")
@Auths
@RestController
class IdeaController(private val service: IdeaService) {
	@Api(
		path = ApiPaths.IDEAS,
		method = RequestMethod.POST,
		summary = "아이디어 생성",
		description = "같은 조합은 저장 결과를 반환하고 새 조합만 AI를 호출합니다.",
	)
	fun create(
		@Valid @RequestBody input: CreateIdeaInDto,
	): ResponseEntity<CreateIdeaOutDto> = service.create(input).let { ResponseEntity.status(if (it.cached) 200 else 201).body(it) }

	@Api(
		path = ApiPaths.IDEA,
		summary = "아이디어 조회",
		description = "저장된 결과를 조회합니다.",
	)
	fun get(
		@PathVariable id: String,
	) = service.get(id)

	@Api(
		path = ApiPaths.RECOMBINE_IDEA,
		method = RequestMethod.POST,
		summary = "다시 조합하기",
		description = "같은 단어로 새 아이디어를 생성합니다.",
	)
	fun recombine(
		@PathVariable id: String,
	) = ResponseEntity.status(201).body(service.recombine(id))
}
