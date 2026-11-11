package com.ideasupport.core.dto.common.basic

open class BasicPageTokenDto(
	open var cursor: String? = null,
	open var size: Int,
)

open class CursorTokenInfo(
	open var id: Long?,
)
