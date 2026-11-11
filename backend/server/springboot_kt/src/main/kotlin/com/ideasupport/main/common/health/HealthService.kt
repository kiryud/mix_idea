package com.ideasupport.main.common.health
import org.springframework.stereotype.Service

@Service
class HealthService(private val health: HealthComponent) {
	fun status() = health.status()
}
