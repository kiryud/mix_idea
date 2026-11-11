package com.ideasupport.main.app.post
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.util.Base64
import com.ideasupport.core.entity.PostEntity
import com.ideasupport.core.dto.app.post.*
import com.ideasupport.infra.repository.*
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class PostComponent(private val posts: PostRepository, private val reactions: ReactionRepository, private val ideas: IdeaRepository) {
	private val random = SecureRandom()

	fun find(
		ideaId: String,
		userIdx: Long,
		scope: String,
	): PostEntity? =
		posts.findByIdeaIdAndUserIdxAndScope(ideaId, userIdx, scope)?.also {
			if (it.hidden) throw ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND)
		}

	@Transactional fun create(
		ideaId: String,
		userIdx: Long,
		scope: String,
	): PostEntity {
		val code = Base64.getUrlEncoder().withoutPadding().encodeToString(ByteArray(24).also { random.nextBytes(it) })
		return posts.saveAndFlush(PostEntity(ideaId = ideaId, userIdx = userIdx, scope = scope, shareCode = code))
	}

	fun shared(code: String): PostEntity =
		posts.findByShareCodeAndHiddenFalse(code) ?: throw ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND)

	@Transactional fun react(
		id: String,
		userIdx: Long,
		kind: String?,
	): SetReactionOutDto {
		if (kind != null && kind !in listOf("like", "meh", "dislike")) throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_INPUT)
		val post =
			posts.findForUpdate(id)?.takeIf {
				!it.hidden && it.scope == "feed"
			} ?: throw ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND)
		if (ideas.findById(post.ideaId).orElse(null)?.hidden != false) throw ServerErrorException(INTERNAL_ERROR_CODE.RESOURCE_NOT_FOUND)
		if (post.userIdx == userIdx) throw ServerErrorException(INTERNAL_ERROR_CODE.OWN_POST_REACTION_FORBIDDEN)
		val existing = reactions.findByPostIdAndUserIdx(id, userIdx)
		if (kind == null) {
			if (existing != null) reactions.delete(existing)
		} else {
			val reaction = existing ?: com.ideasupport.core.entity.ReactionEntity(postId = id, userIdx = userIdx)
			reaction.kind = kind
			reaction.updatedAt = java.time.Instant.now()
			reactions.save(reaction)
		}
		reactions.flush()
		return SetReactionOutDto(
			id,
			kind,
			ReactionCountsOutDto(
				reactions.countByPostIdAndKind(id, "like"),
				reactions.countByPostIdAndKind(id, "meh"),
				reactions.countByPostIdAndKind(id, "dislike"),
			),
		)
	}
}
