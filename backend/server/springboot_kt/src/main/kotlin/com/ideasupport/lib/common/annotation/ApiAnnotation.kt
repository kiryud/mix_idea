package com.ideasupport.lib.common.annotation

import io.swagger.v3.oas.annotations.Operation
import org.springframework.core.annotation.AliasFor
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@RequestMapping
@Operation
annotation class Api(
	@get:AliasFor(annotation = RequestMapping::class, attribute = "path")
	val path: String = "",
	@get:AliasFor(annotation = RequestMapping::class, attribute = "method")
	val method: RequestMethod = RequestMethod.GET,
	@get:AliasFor(annotation = Operation::class, attribute = "summary")
	val summary: String = "",
	@get:AliasFor(annotation = Operation::class, attribute = "description")
	val description: String = "",
)
