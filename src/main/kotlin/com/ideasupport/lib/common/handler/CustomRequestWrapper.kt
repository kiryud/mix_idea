package com.ideasupport.lib.common.handler

import jakarta.servlet.*
import jakarta.servlet.annotation.WebFilter
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.Part
import java.io.*
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import org.apache.commons.lang3.StringUtils
import org.zeroturnaround.zip.commons.IOUtils

class CustomRequestWrapper(request: HttpServletRequest) : HttpServletRequestWrapper(request) {
	private val encoding: Charset
	private val rawData: ByteArray
	private val parts: MutableMap<String, Part> = mutableMapOf()

	init {
		var characterEncoding = request.characterEncoding
		if (StringUtils.isBlank(characterEncoding)) {
			characterEncoding = StandardCharsets.UTF_8.name()
		}
		this.encoding = Charset.forName(characterEncoding)

		try {
			val inputStream: InputStream = request.inputStream
			this.rawData = IOUtils.toByteArray(inputStream)

			if (request.contentType != null && request.contentType.startsWith("multipart/")) {
				// 멀티파트 요청일 경우에만 Part 객체들을 저장
				val collection = request.parts
				for (part in collection) {
					parts[part.name] = part
				}
			}
		} catch (e: IOException) {
			throw e
		}
	}

	@Throws(IOException::class)
	override fun getInputStream(): ServletInputStream {
		val byteArrayInputStream = ByteArrayInputStream(this.rawData)
		val servletInputStream: ServletInputStream =
			object : ServletInputStream() {
				override fun isFinished(): Boolean {
					return false
				}

				override fun isReady(): Boolean {
					return false
				}

				override fun setReadListener(readListener: ReadListener) {
				}

				@Throws(IOException::class)
				override fun read(): Int {
					return byteArrayInputStream.read()
				}
			}
		return servletInputStream
	}

	@Throws(IOException::class)
	override fun getReader(): BufferedReader {
		return BufferedReader(InputStreamReader(this.inputStream, this.encoding))
	}

	override fun getRequest(): ServletRequest {
		return super.getRequest()
	}

	override fun getPart(name: String): Part? {
		return parts[name]
	}

	override fun getParts(): Collection<Part> {
		return if (parts.isNotEmpty()) parts.values else super.getParts()
	}
}

@WebFilter(urlPatterns = ["*"])
class RequestFilter : Filter {
	@Throws(IOException::class, ServletException::class)
	override fun doFilter(
		request: ServletRequest?,
		response: ServletResponse?,
		chain: FilterChain,
	) {
		if (request is HttpServletRequest && response is HttpServletResponse) {
			val contentType = request.contentType
			if (contentType != null && contentType.startsWith("multipart/")) {
				// multipart 요청이면 필터링을 건너뛰기
				chain.doFilter(request, response)
			} else {
				val rereadableRequestWrapper = CustomRequestWrapper(request)
				chain.doFilter(rereadableRequestWrapper, response)
			}
		} else {
			chain.doFilter(request, response)
		}
	}
}
