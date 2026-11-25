package com.ideasupport.main.app.category
import com.ideasupport.core.dto.app.category.*
import com.ideasupport.lib.common.annotation.Api
import com.ideasupport.lib.common.annotation.Auths
import com.ideasupport.lib.common.objects.ApiPaths
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Category", description = "아이디어 보조 category API")
@Auths
@RestController
class CategoryController(private val service: CategoryService) {
	@Api(path = ApiPaths.CATEGORIES, summary = "카테고리 목록", description = "기본 및 사용자 카테고리와 단어 수를 조회합니다.")
	fun list() = service.list()

	@Api(path = ApiPaths.CATEGORY_PREVIEW, method = RequestMethod.POST, summary = "카테고리 미리보기", description = "이름과 관련된 기존 단어를 조회합니다.")
	fun preview(
		@Valid @RequestBody input: CategoryNameInDto,
	) = service.preview(input)

	@Api(path = ApiPaths.CATEGORIES, method = RequestMethod.POST, summary = "카테고리 추가", description = "중복 이름은 재사용하고 관련 단어를 연결합니다.")
	fun create(
		@Valid @RequestBody input: CategoryNameInDto,
	): ResponseEntity<CreateCategoryOutDto> = service.create(input).let { ResponseEntity.status(if (it.created) 201 else 200).body(it) }
}
