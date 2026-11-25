package com.ideasupport.lib.common.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.multipart.MultipartHttpServletRequest
import com.ideasupport.core.entity.IpRange
import com.ideasupport.core.entity.IpType
import com.ideasupport.infra.repository.IpRepository

@Component
class Etc(
	@Autowired
	private val env: Environment,
	private val memory: Memory,
	private val ipRepository: IpRepository,
) {
	fun substringEmail(email: String): String {
		return email.split("@").firstOrNull().toString()
	}

	fun getActiveProfile(): String {
		return if (env.activeProfiles.isNotEmpty()) {
			env.activeProfiles.joinToString()
		} else {
			""
		}
	}

	fun getIp(request: HttpServletRequest): String? {
		val ip =
			request.getHeader("X-Forwarded-For")
				?: request.getHeader("Proxy-Client-IP")
				?: request.getHeader("WL-Proxy-Client-IP")
				?: request.getHeader("HTTP_X_FORWARDED_FOR")
				?: request.getHeader("HTTP_X_FORWARDED")
				?: request.getHeader("HTTP_X_CLUSTER_CLIENT_IP")
				?: request.getHeader("HTTP_CLIENT_IP")
				?: request.getHeader("HTTP_FORWARDED")
				?: request.getHeader("X-Real-IP")
				?: request.getHeader("X-RealIP")
				?: request.getHeader("REMOTE_ADDR")
				?: request.remoteAddr

		return ip
	}

	fun initIp() {
		val ips = ipRepository.findAll()
		memory.saveAllowAdminIp(ips.filter { it.ipType == IpType.ALLOW && it.ipRange == IpRange.ADMIN }.map { it.ip!! })
		memory.saveDenyAdminIp(ips.filter { it.ipType == IpType.DENY && it.ipRange == IpRange.ADMIN }.map { it.ip!! })
		memory.saveAllowUserIp(ips.filter { it.ipType == IpType.ALLOW && it.ipRange == IpRange.USER }.map { it.ip!! })
		memory.saveDenyUserIp(ips.filter { it.ipType == IpType.DENY && it.ipRange == IpRange.USER }.map { it.ip!! })
		memory.saveAllowAllIp(ips.filter { it.ipType == IpType.ALLOW && it.ipRange == IpRange.ALL }.map { it.ip!! })
		memory.saveDenyAllIp(ips.filter { it.ipType == IpType.DENY && it.ipRange == IpRange.ALL }.map { it.ip!! })
	}

	fun getFileName(request: HttpServletRequest): String {
		val multipartFile: MultipartFile? = (request as? MultipartHttpServletRequest)?.fileMap?.values?.firstOrNull()
		val filename = multipartFile?.originalFilename ?: "FILE"

		return filename
	}

	fun getRequestPath(request: HttpServletRequest): String {
		val path = request.requestURI
		val query = request.queryString
		return if (query != null) {
			"$path?$query"
		} else {
			path
		}
	}

	fun getRequestBody(request: HttpServletRequest): String {
		if (request.requestURI.startsWith("/app/user/") || request.requestURI.startsWith("/app/dev/")) return "[REDACTED]"
		return if (request.contentType != null && request.contentType.startsWith("multipart/")) {
			getFileName(request)
		} else {
			request.inputStream.bufferedReader().use { it.readText() }
		}
	}
}
