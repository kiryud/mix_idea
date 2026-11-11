package com.ideasupport.main.app.post
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import jakarta.validation.Valid
import io.swagger.v3.oas.annotations.tags.Tag
import com.ideasupport.lib.common.annotation.*
import com.ideasupport.lib.common.objects.ApiPaths
import com.ideasupport.core.dto.app.post.*

@Tag(name = "Post", description = "공유 피드와 반응")
@Auths
@RestController
class PostController(private val service: PostService) {
	@Api(path = ApiPaths.SHARES, method = RequestMethod.POST, summary = "링크 공유", description = "피드에 노출하지 않는 공유 링크를 생성합니다.")
	fun share(
		@Valid @RequestBody input: CreatePostInDto,
	): ResponseEntity<CreatePostOutDto> = response(service.create(input, "link"))

	@Api(path = ApiPaths.POSTS, method = RequestMethod.POST, summary = "피드 게시", description = "완성된 아이디어를 공개 피드에 게시합니다.")
	fun post(
		@Valid @RequestBody input: CreatePostInDto,
	): ResponseEntity<CreatePostOutDto> = response(service.create(input, "feed"))

	@Api(path = ApiPaths.POSTS, summary = "피드 조회", description = "최신순·인기순 커서 목록과 내 반응을 조회합니다.")
	fun list(
		@RequestParam(defaultValue = "latest") sort: String,
		@RequestParam(defaultValue = "20") limit: Int,
		@RequestParam(required = false) cursor: String?,
	) = service.list(sort, limit, cursor)

	@Api(
		path = ApiPaths.POST_REACTION,
		method = RequestMethod.PUT,
		summary = "반응 변경",
		description = "반응 1개를 설정하고 null이면 취소합니다. 내 글에는 반응할 수 없습니다.",
	)
	fun react(
		@PathVariable id: String,
		@RequestBody input: SetReactionInDto,
	) = service.react(id, input)

	private fun response(output: CreatePostOutDto): ResponseEntity<CreatePostOutDto> =
		ResponseEntity.status(
			if (output.created) 201 else 200,
		).body(output)
}
