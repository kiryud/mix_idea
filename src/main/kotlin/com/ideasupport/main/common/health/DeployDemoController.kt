package com.ideasupport.main.common.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class DeployDemoController {
	@GetMapping("/common/deploy-demo")
	fun version(): Map<String, String> =
		mapOf(
			"version" to "team-v1",
			"pod" to (System.getenv("HOSTNAME") ?: "local"),
		)
}
