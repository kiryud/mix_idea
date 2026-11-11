package com.ideasupport.lib.common.annotation

import org.aspectj.lang.JoinPoint
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.aspectj.lang.annotation.Pointcut
import org.aspectj.lang.reflect.MethodSignature
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import com.ideasupport.core.entity.IpRange
import com.ideasupport.core.entity.IpType
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.lib.common.objects.Profiles
import com.ideasupport.lib.common.util.Etc
import com.ideasupport.lib.common.util.Memory

/**
 * [IpAuth]
 * - ip 허용 또는 제한 처리하는 annotation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class IpAuth(
	val ipType: IpType,
	val ipRange: IpRange,
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class IpAuths(
	val ipType: IpType,
	val ipRange: IpRange,
)

@Aspect
@Component
@Profile(Profiles.DEV_OR_PROD)
class IpAuthAspect(
	private val memory: Memory,
	private val etc: Etc,
) {
	@Pointcut("(@annotation(IpAuth) || @within(IpAuths)) && execution(* *(..))")
	fun ipAuthPointcut() {}

	@Before("ipAuthPointcut()")
	fun ipAuthBefore(joinPoint: JoinPoint) {
		val request = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request
		val methodSignature = joinPoint.signature as MethodSignature
		val targetClass = methodSignature.method.declaringClass
		val ipAuth = targetClass.getAnnotation(IpAuths::class.java)
		val ipAuths = methodSignature.method.getAnnotation(IpAuth::class.java)
		val annotation = ipAuth ?: ipAuths ?: return
		val (ipType, ipRange) =
			when (annotation) {
				is IpAuth -> Pair(annotation.ipType, annotation.ipRange)
				is IpAuths -> Pair(annotation.ipType, annotation.ipRange)
				else -> return
			}

		val clientIp = etc.getIp(request)
		when (ipType) {
			IpType.ALLOW -> {
				val allowIps =
					when (ipRange) {
						IpRange.ADMIN -> {
							memory.getAllowAdminIp()
						}
						IpRange.USER -> {
							memory.getAllowUserIp()
						}
						IpRange.ALL -> {
							memory.getAllowAllIp()
						}
					}
				if (!allowIps.contains(clientIp)) {
					throw ServerErrorException(INTERNAL_ERROR_CODE.NOT_ALLOW_IP)
				}
			}
			IpType.DENY -> {
				val denyIps =
					when (ipRange) {
						IpRange.ADMIN -> {
							memory.getDenyAdminIp()
						}
						IpRange.USER -> {
							memory.getDenyUserIp()
						}
						IpRange.ALL -> {
							memory.getDenyAllIp()
						}
					}

				if (denyIps.contains(clientIp)) {
					throw ServerErrorException(INTERNAL_ERROR_CODE.DENY_IP)
				}
			}
		}
	}
}
