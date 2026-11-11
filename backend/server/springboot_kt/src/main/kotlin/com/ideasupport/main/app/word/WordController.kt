package com.ideasupport.main.app.word
import com.ideasupport.core.dto.app.word.*
import com.ideasupport.lib.common.annotation.Api
import com.ideasupport.lib.common.annotation.Auths
import com.ideasupport.lib.common.objects.ApiPaths
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@Tag(name = "Word", description = "아이디어 보조 word API")
@Auths
@RestController
class WordController(private val service: WordService) {
	@Api(path = ApiPaths.DRAW_WORDS, method = RequestMethod.POST, summary = "단어 3개 추출", description = "최대 2개 단어를 고정하고 방향성에 맞게 다시 뽑습니다.")
	fun draw(
		@Valid @RequestBody input: DrawWordsInDto,
	) = service.draw(input)
}
