package com.ideasupport.main.common.ip

import org.springframework.stereotype.Service
import com.ideasupport.lib.common.util.Etc

@Service
class IpService(
	private val etc: Etc,
) {
	fun initIp() {
		etc.initIp()
	}
}
