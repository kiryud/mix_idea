package com.ideasupport.main.app.post
import org.springframework.stereotype.Component
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import com.fasterxml.jackson.databind.ObjectMapper
import java.sql.Timestamp
import java.util.Base64
import com.ideasupport.core.dto.app.post.*
import com.ideasupport.infra.repository.IdeaWordRepository
import com.ideasupport.infra.repository.WordRepository
import com.ideasupport.lib.common.component.ContentPolicyComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class FeedComponent(
	private val jdbc: NamedParameterJdbcTemplate,
	private val mapper: ObjectMapper,
	private val links: IdeaWordRepository,
	private val words: WordRepository,
	private val policy: ContentPolicyComponent,
) {
	fun list(
		sort: String,
		limit: Int,
		cursor: String?,
		userIdx: Long,
	): GetPostsOutDto {
		policy.requireInput(sort in listOf("latest", "popular") && limit in 1..50)
		val after = cursor?.let { decode(it, sort) }
		val score = "(COALESCE(r.likes,0)-COALESCE(r.dislikes,0))"
		val scoreClause = if (sort == "popular") "$score < :score OR ($score = :score AND " else ""
		val filter = if (after == null) "" else "AND ($scoreClause (p.created_at < :time OR (p.created_at = :time AND p.id < :id)) ${if (sort == "popular") ")" else ""})"
		val order = if (sort == "popular") "$score DESC, " else ""
		val query =
			"""
			SELECT p.id, p.idea_id, p.user_idx, p.created_at, i.title, i.content, COALESCE(u.name,'사용자') AS author_name,
			 COALESCE(r.likes,0) AS likes, COALESCE(r.mehs,0) AS mehs, COALESCE(r.dislikes,0) AS dislikes, mine.kind AS my_reaction
			FROM posts p JOIN ideas i ON i.id = p.idea_id JOIN users u ON u.idx = p.user_idx
			LEFT JOIN (SELECT post_id, SUM(CASE WHEN kind='like' THEN 1 ELSE 0 END) AS likes, SUM(CASE WHEN kind='meh' THEN 1 ELSE 0 END) AS mehs, SUM(CASE WHEN kind='dislike' THEN 1 ELSE 0 END) AS dislikes FROM reactions GROUP BY post_id) r ON r.post_id = p.id
			LEFT JOIN reactions mine ON mine.post_id = p.id AND mine.user_idx = :user
			WHERE p.scope='feed' AND p.hidden=false AND i.hidden=false $filter
			ORDER BY ${order}p.created_at DESC, p.id DESC LIMIT :limit
			""".trimIndent()
		val params = mutableMapOf<String, Any>("user" to userIdx, "limit" to limit + 1)
		if (after != null) {
			params["score"] = after.score
			params["time"] = Timestamp.from(after.createdAt)
			params["id"] = after.id
		}
		val rows =
			jdbc.query(
				query,
				params,
			) { rs, _ ->
				PostOutDto(
					rs.getString(
						"id",
					),
					rs.getString(
						"idea_id",
					),
					rs.getString(
						"title",
					),
					rs.getString(
						"content",
					),
					emptyList(),
					rs.getString(
						"author_name",
					),
					rs.getLong("user_idx") == userIdx,
					rs.getString(
						"my_reaction",
					),
					ReactionCountsOutDto(rs.getLong("likes"), rs.getLong("mehs"), rs.getLong("dislikes")), rs.getTimestamp("created_at").toInstant(),
				)
			}
		val page = rows.take(limit)
		val linked = if (page.isEmpty()) emptyList() else links.findByIdeaIdIn(page.map { it.ideaId })
		val texts = words.findAllById(linked.map { it.wordId }.distinct()).associate { it.id to it.text }
		val grouped = linked.groupBy { it.ideaId }
		val items =
			page.map {
					item ->
				item.copy(words = grouped[item.ideaId].orEmpty().sortedBy { it.position }.map { texts.getValue(it.wordId) })
			}
		val next =
			if (rows.size > limit) {
				page.last().let {
					encode(FeedCursorDto(sort, it.reactions.like - it.reactions.dislike, it.createdAt, it.id))
				}
			} else {
				null
			}
		return GetPostsOutDto(items, next)
	}

	private fun encode(cursor: FeedCursorDto): String =
		Base64.getUrlEncoder().withoutPadding().encodeToString(
			mapper.writeValueAsBytes(cursor),
		)

	private fun decode(
		value: String,
		sort: String,
	): FeedCursorDto =
		try {
			policy.requireInput(value.length <= 1024)
			mapper.readValue(Base64.getUrlDecoder().decode(value), FeedCursorDto::class.java).also {
				policy.requireInput(it.sort == sort && it.id.matches(Regex("[0-9a-f-]{36}")))
			}
		} catch (ex: Exception) {
			throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_INPUT)
		}
}
