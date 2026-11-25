package com.ideasupport.core.dto.app.category
import jakarta.validation.constraints.*
import com.ideasupport.core.dto.app.word.WordOutDto

data class CategoryNameInDto(
	@field:NotBlank @field:Size(max = 50) val name: String,
	@field:Pattern(regexp = "food|software|contest|general") val guideType: String = "general",
)

data class CategoryOutDto(val id: String, val name: String, val kind: String, val wordCount: Long, val useCount: Long, val supportsRecipe: Boolean = false, val guideType: String = "general", val guideLabel: String = "실행 계획")

data class GetCategoriesOutDto(val items: List<CategoryOutDto>)

data class PreviewCategoryOutDto(val name: String, val existingCategoryId: String?, val canCreate: Boolean, val matchedWordCount: Int, val words: List<WordOutDto>, val matchSource: String, val guideType: String = "general", val guideLabel: String = "실행 계획")

data class CreateCategoryOutDto(val category: CategoryOutDto, val created: Boolean)
