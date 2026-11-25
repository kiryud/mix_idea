package com.ideasupport.main.app.word
import org.springframework.stereotype.Component
import com.ideasupport.core.entity.*
import com.ideasupport.core.dto.app.word.WordOutDto
import com.ideasupport.infra.ai.AiClient
import com.ideasupport.lib.common.component.*

@Component
class WordMatchComponent(
	private val ai: AiClient,
	private val policy: ContentPolicyComponent,
	private val cache: WordMatchCacheComponent,
	private val coordinator: GenerationCoordinatorComponent,
	private val limits: RequestLimitComponent,
) {
	fun match(
		input: String,
		pool: List<WordEntity>,
		minimum: Int,
		userIdx: Long,
	): WordMatchResult {
		policy.check(input)
		val tokens = policy.normalize(input).split(" ").filter { it.isNotBlank() }.map { policy.key(it) }
		val ranked =
			pool.map {
					word ->
				word to
					tokens.count {
							token ->
						policy.key(word.text + " " + word.keywords).contains(token)
					}
			}.filter { it.second > 0 }.sortedWith(compareByDescending<Pair<WordEntity, Int>> { it.second }.thenBy { it.first.id })
		val best = ranked.filter { it.second == ranked.firstOrNull()?.second }
		val direct = (if (best.size >= minimum) best else ranked).map { it.first }.take(40)
		if (direct.size >= minimum || ai.source == "mock") return WordMatchResult(direct, if (ai.source == "mock") "mock" else "keyword")
		val key =
			policy.hash(
				"match-v2|${ai.cacheVersion}|${policy.key(input)}|" +
					pool.sortedBy {
						it.id
					}.joinToString { it.id + ":" + it.text + ":" + it.keywords },
			)

		fun read(): WordMatchResult? = cache.get(key)?.let { ids -> WordMatchResult(pool.filter { it.id in ids }, "cache") }
		read()?.let { return it }
		return coordinator.execute("match:$key") {
			read() ?: run {
				limits.check("preview", userIdx)
				val candidates = (direct + pool.sortedBy { it.id }).distinctBy { it.id }.take(400)
				val selected = ai.selectWords(input, candidates.map { WordOutDto(it.id, it.text) })
				if (selected.distinct().size != selected.size || selected.size > 40) throw com.ideasupport.lib.common.exception.ServerErrorException(com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE.AI_RESPONSE_INVALID)
				if (selected.any {
							id ->
						candidates.none { it.id == id }
					}
				) {
					throw com.ideasupport.lib.common.exception.ServerErrorException(com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE.AI_RESPONSE_INVALID)
				}
				cache.put(key, input, selected)
				WordMatchResult(candidates.filter { it.id in selected }, "ai")
			}
		}
	}
}
