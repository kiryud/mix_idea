package com.ideasupport.core.dto.app.idea
import jakarta.validation.constraints.*
import java.time.Instant
import com.ideasupport.core.dto.app.word.WordOutDto

data class CreateIdeaInDto(
	@field:NotBlank val categoryId: String,
	@field:Size(min = 3, max = 3) val wordIds: List<String>,
	@field:Size(max = 200) val direction: String = "",
)

data class IdeaOutDto(val id: String, val categoryId: String, val words: List<WordOutDto>, val direction: String, val title: String, val content: String, val parentIdeaId: String?, val source: String, val hasRecipe: Boolean, val createdAt: Instant, val supportsRecipe: Boolean = false, val guideType: String = "general", val guideLabel: String = "실행 계획", val hasGuide: Boolean = false)

data class GetIdeaOutDto(val idea: IdeaOutDto)

data class CreateIdeaOutDto(val idea: IdeaOutDto, val cached: Boolean)

data class GeneratedIdeaDto(val title: String, val content: String)
