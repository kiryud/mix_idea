package com.ideasupport.lib.common.component

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component
import com.ideasupport.lib.common.util.Etc

@Component
class PostConstructComponent(
	private val etc: Etc,
) {
	@PostConstruct
	fun init() {
		etc.initIp()
	}
}
