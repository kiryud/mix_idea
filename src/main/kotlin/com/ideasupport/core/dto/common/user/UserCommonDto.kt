package com.ideasupport.core.dto.common.user

import java.time.Instant

open class UserCommonDto(
	open var idx: Long? = null,
	open var id: String? = null,
	open var pw: String? = null,
	open var uid: String? = null,
	open var email: String? = null,
	open var name: String? = null,
	open var createdAt: Instant? = null,
	open var updatedAt: Instant? = null,
)
