package com.ideasupport.main.scheduler

import org.springframework.stereotype.Service
import com.ideasupport.lib.common.annotation.Scheduler
import com.ideasupport.lib.common.util.Etc

@Service
class IpScheduler(
	private val etc: Etc,
) {
	@Scheduler(initialDelay = 0, fixedDelayString = "300000")
	fun initIp() {
		etc.initIp()
	}
}
