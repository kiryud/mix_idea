package com.ideasupport.main.app.share
import org.springframework.stereotype.Service
import com.ideasupport.main.app.post.PostComponent
import com.ideasupport.main.app.idea.IdeaStorageComponent
import com.ideasupport.main.app.guide.GuideStorageComponent
import com.ideasupport.main.app.guide.GuideType

@Service
class ShareService(
	private val posts: PostComponent,
	private val ideas: IdeaStorageComponent,
	private val guides: GuideStorageComponent,
	private val html: ShareHtmlComponent,
) {
	fun page(code: String): String {
		val post = posts.shared(code)
		val idea = ideas.get(post.ideaId)
		val output = ideas.output(idea)
		val guide = guides.find(idea.id, GuideType.from(output.guideType))?.let { guides.output(it) }
		return html.render(output, guide)
	}
}
