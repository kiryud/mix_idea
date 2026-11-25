package com.ideasupport.core.dto.app.post
import jakarta.validation.constraints.NotBlank
import java.time.Instant

data class CreatePostInDto(
	@field:NotBlank val ideaId: String,
)

data class CreatePostOutDto(val postId: String, val scope: String, val shareCode: String, val shareUrl: String, val created: Boolean)

data class SetReactionInDto(val kind: String?)

data class ReactionCountsOutDto(val like: Long, val meh: Long, val dislike: Long)

data class SetReactionOutDto(val postId: String, val myReaction: String?, val reactions: ReactionCountsOutDto)

data class PostOutDto(val id: String, val ideaId: String, val title: String, val content: String, val words: List<String>, val authorName: String, val isMine: Boolean, val myReaction: String?, val reactions: ReactionCountsOutDto, val createdAt: Instant)

data class GetPostsOutDto(val items: List<PostOutDto>, val nextCursor: String?)

data class FeedCursorDto(val sort: String, val score: Long, val createdAt: Instant, val id: String)
