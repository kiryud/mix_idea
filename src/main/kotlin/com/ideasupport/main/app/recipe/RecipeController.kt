package com.ideasupport.main.app.recipe
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import io.swagger.v3.oas.annotations.tags.Tag
import com.ideasupport.lib.common.annotation.*
import com.ideasupport.lib.common.objects.ApiPaths
import com.ideasupport.core.dto.app.recipe.CreateRecipeOutDto

@Tag(name = "Recipe", description = "만드는 법")
@Auths
@RestController
class RecipeController(private val service: RecipeService) {
	@Api(
		path = ApiPaths.IDEA_RECIPE,
		method = RequestMethod.POST,
		summary = "분야별 후속 가이드 (기존 경로)",
		description = "기존 recipe 응답 구조를 유지하는 호환 경로입니다. 신규 연동은 /app/ideas/{id}/guide를 사용합니다.",
	)
	fun create(
		@PathVariable id: String,
	): ResponseEntity<CreateRecipeOutDto> = service.create(id).let { ResponseEntity.status(if (it.cached) 200 else 201).body(it) }
}
