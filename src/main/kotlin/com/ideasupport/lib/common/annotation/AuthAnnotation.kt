package com.ideasupport.lib.common.annotation

import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.stereotype.Component
import com.ideasupport.core.entity.UserRole
import com.ideasupport.lib.common.component.AuthComponent

/**
 * [Auth]
 * - request header 에서 토큰을 가져와서 인증을 처리하는 annotation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "JWT")
annotation class Auth(
	val role: UserRole = UserRole.USER,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@SecurityRequirement(name = "JWT")
annotation class Auths(
	val role: UserRole = UserRole.USER,
)

@Aspect
@Component
class AuthAspect(
	private val authComponent: AuthComponent,
) {
	@Pointcut("(@annotation(Auth) || @within(Auths)) && execution(* *(..))")
	fun authPointcut() {}

	@Before("authPointcut()")
	fun authBefore(joinPoint: JoinPoint) {
		val methodSignature = joinPoint.signature as MethodSignature
		val targetClass = methodSignature.method.declaringClass
		val auth = targetClass.getAnnotation(Auths::class.java)
		val auths = methodSignature.method.getAnnotation(Auth::class.java)
		val annotation = auth ?: auths ?: return
		val role =
			when (annotation) {
				is Auth -> annotation.role
				is Auths -> annotation.role
				else -> return
			}

		authComponent.getUserTokenInfo(role)
	}
}
