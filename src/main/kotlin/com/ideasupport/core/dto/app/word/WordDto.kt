package com.ideasupport.core.dto.app.word
import jakarta.validation.constraints.*

data class WordOutDto(val id: String, val text: String)

data class DrawWordsInDto(
	@field:NotBlank val categoryId: String,
	@field:Size(max = 2) val lockedWordIds: List<String> = emptyList(),
	@field:Size(max = 3) val previousWordIds: List<String> = emptyList(),
	@field:Size(max = 200) val direction: String = "",
)

data class DrawWordOutDto(val id: String, val text: String, val locked: Boolean)

data class DrawWordsOutDto(val words: List<DrawWordOutDto>, val direction: String, val matchSource: String)
