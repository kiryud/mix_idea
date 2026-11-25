package com.ideasupport

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.ServletComponentScan
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
@ServletComponentScan
class IdeaSupportApplication

fun main(args: Array<String>) {
	runApplication<IdeaSupportApplication>(*args)
}
