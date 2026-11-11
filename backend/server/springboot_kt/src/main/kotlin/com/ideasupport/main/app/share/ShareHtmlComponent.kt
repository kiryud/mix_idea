package com.ideasupport.main.app.share
import org.springframework.stereotype.Component
import org.springframework.web.util.HtmlUtils
import com.ideasupport.core.dto.app.idea.IdeaOutDto
import com.ideasupport.core.dto.app.guide.GuideOutDto

@Component
class ShareHtmlComponent {
	private fun escape(value: String) = HtmlUtils.htmlEscape(value, "UTF-8")

	fun render(
		idea: IdeaOutDto,
		guide: GuideOutDto?,
	): String {
		val instructions =
			guide?.takeIf { it.kind == idea.guideType }?.let {
					row ->
				"<section><h2>${escape(row.label)}</h2><p>${escape(row.summary)}</p><h3>${escape(row.requirementsLabel)}</h3><ul>${row.requirements.joinToString("") { "<li>${escape(it.name)}: ${escape(it.detail)}</li>" }}</ul><ol>${row.steps.joinToString("") { "<li><strong>${escape(it.title)}</strong><p>${escape(it.description)}</p></li>" }}</ol></section>"
			} ?: ""
		return """<!doctype html><html lang="ko"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escape(
			idea.title,
		)}</title><style>body{font:17px/1.7 system-ui,sans-serif;max-width:720px;margin:40px auto;padding:0 24px;color:#202630}article{white-space:pre-wrap}small{color:#667085}</style></head><body><main><small>아이디어 보조 · ${if (idea.source == "mock") "시연 예시" else "공유된 아이디어"}</small><h1>${escape(
			idea.title,
		)}</h1><p>${idea.words.joinToString(
			" · ",
		) { escape(it.text) }}</p><article>${escape(idea.content)}</article>$instructions</main></body></html>"""
	}
}
