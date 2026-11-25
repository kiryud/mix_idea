package com.ideasupport.main.app.guide

import com.ideasupport.core.dto.app.guide.CreateGuideOutDto
import com.ideasupport.main.app.idea.IdeaStorageComponent
import com.ideasupport.main.app.category.CategoryComponent
import com.ideasupport.main.app.user.UserIdentityComponent
import com.ideasupport.lib.common.component.GenerationCoordinatorComponent
import com.ideasupport.lib.common.component.RequestLimitComponent
import org.springframework.stereotype.Service

@Service
class GuideService(
	private val storage: GuideStorageComponent,
	private val generation: GuideGenerationComponent,
	private val ideas: IdeaStorageComponent,
	private val categories: CategoryComponent,
	private val types: GuideTypeComponent,
	private val identity: UserIdentityComponent,
	private val coordinator: GenerationCoordinatorComponent,
	private val limits: RequestLimitComponent,
) {
	fun create(id: String): CreateGuideOutDto {
		val user = identity.current()
		val idea = ideas.get(id)
		val category = categories.get(idea.categoryId)
		val type = types.of(category)
		storage.find(id, type)?.let { return CreateGuideOutDto(storage.output(it), true) }
		var created = false
		val result =
			coordinator.execute("guide:$id:${type.value}") {
				storage.find(id, type) ?: run {
					limits.check("recipe", user.idx!!)
					val output = generation.generate(type, category.name, idea.title, idea.content)
					storage.save(id, output, generation.source).also { created = true }
				}
			}
		return CreateGuideOutDto(storage.output(result), !created)
	}
}
