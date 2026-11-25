package com.ideasupport.lib.common.util

import org.springframework.stereotype.Service

@Service
class Memory {
	private var uuid: String? = null
	private var allowAdminIp: List<String> = emptyList()
	private var denyAdminIp: List<String> = emptyList()
	private var allowUserIp: List<String> = emptyList()
	private var denyUserIp: List<String> = emptyList()
	private var allowAllIp: List<String> = emptyList()
	private var denyAllIp: List<String> = emptyList()

	fun saveUUID(value: String) {
		uuid = value
	}

	fun getUUID(): String? {
		return uuid
	}

	fun clearUUID() {
		uuid = null
	}

	fun saveAllowAdminIp(value: List<String>) {
		allowAdminIp = value
	}

	fun getAllowAdminIp(): List<String> {
		return allowAdminIp
	}

	fun saveDenyAdminIp(value: List<String>) {
		denyAdminIp = value
	}

	fun getDenyAdminIp(): List<String> {
		return denyAdminIp
	}

	fun saveAllowUserIp(value: List<String>) {
		allowUserIp = value
	}

	fun getAllowUserIp(): List<String> {
		return allowUserIp
	}

	fun saveDenyUserIp(value: List<String>) {
		denyUserIp = value
	}

	fun getDenyUserIp(): List<String> {
		return denyUserIp
	}

	fun saveAllowAllIp(value: List<String>) {
		allowAllIp = value
	}

	fun getAllowAllIp(): List<String> {
		return allowAllIp
	}

	fun saveDenyAllIp(map: List<String>): List<String> {
		return denyAllIp
	}

	fun getDenyAllIp(): List<String> {
		return denyAllIp
	}
}
