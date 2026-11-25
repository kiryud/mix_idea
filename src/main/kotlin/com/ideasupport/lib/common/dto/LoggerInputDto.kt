package com.ideasupport.lib.common.dto

data class LoggerInputDto(
	val uid: String,
	val name: String,
	val email: String,
	val fileName: String? = null,
	val funcName: String? = null,
	val code: Int? = null,
	val errorName: String? = null,
	val errorMessage: String? = null,
	val message: String?,
	val method: String?,
	val path: String?,
	val body: String? = null,
	val ip: String? = null,
	val referer: String? = null,
	val headers: Map<String, String>? = null,
)
