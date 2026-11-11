package com.ideasupport.main.app.post
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataIntegrityViolationException
import com.ideasupport.core.dto.app.post.*
import com.ideasupport.main.app.idea.IdeaStorageComponent
import com.ideasupport.main.app.user.UserIdentityComponent
import com.ideasupport.lib.common.component.RequestLimitComponent

@Service
class PostService(
	private val posts: PostComponent,
	private val feed: FeedComponent,
	private val ideas: IdeaStorageComponent,
	private val identity: UserIdentityComponent,
	private val limits: RequestLimitComponent,
	@Value("\${app.public-url:http://localhost:8080}") private val publicUrl: String,
) {
	fun create(
		input: CreatePostInDto,
		scope: String,
	): CreatePostOutDto {
		val user = identity.current()
		ideas.get(input.ideaId)
		var created = false
		val post =
			posts.find(input.ideaId, user.idx!!, scope) ?: run {
				limits.check("post", user.idx!!)
				try {
					posts.create(input.ideaId, user.idx!!, scope).also { created = true }
				} catch (ex: DataIntegrityViolationException) {
					posts.find(input.ideaId, user.idx!!, scope) ?: throw ex
				}
			}
		return CreatePostOutDto(post.id, post.scope, post.shareCode, "${publicUrl.trimEnd('/')}/s/${post.shareCode}", created)
	}

	fun list(
		sort: String,
		limit: Int,
		cursor: String?,
	) = feed.list(sort, limit, cursor, identity.current().idx!!)

	fun react(
		id: String,
		input: SetReactionInDto,
	): SetReactionOutDto {
		val user = identity.current()
		limits.check("reaction", user.idx!!)
		return posts.react(id, user.idx!!, input.kind)
	}
}
