package com.ideasupport.core.dto.app.guide

data class GuideRequirementDto(val name: String, val detail: String)

data class GuideStepDto(val order: Int, val title: String, val description: String)

data class GeneratedGuideDto(val kind: String, val feasible: Boolean, val summary: String, val requirements: List<GuideRequirementDto>, val steps: List<GuideStepDto>)

data class GuideOutDto(val ideaId: String, val kind: String, val label: String, val requirementsLabel: String, val feasible: Boolean, val summary: String, val requirements: List<GuideRequirementDto>, val steps: List<GuideStepDto>, val source: String)

data class CreateGuideOutDto(val guide: GuideOutDto, val cached: Boolean)
