package com.ideasupport.lib.common.annotation

import org.aspectj.lang.annotation.After
import org.aspectj.lang.annotation.Aspect
import org.aspectj.lang.annotation.Before
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.lib.common.component.AuthComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException
import com.ideasupport.lib.common.objects.Profiles

/**
 * [UserLevelLock]
 * - user level lock 을 위한 annotation
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class UserLevelLock

@Aspect
@Component
@Profile(Profiles.NOT_TEST)
class UserLevelLockAspect(
	private val authComponent: AuthComponent,
	private val userRepository: UserRepository,
) {
	@Before("@annotation(UserLevelLock) && execution(* *(..))")
	fun userLevelLockBefore() {
		val userTokenInfo = authComponent.getUserTokenInfo()
		val uid = userTokenInfo.uid
		val getLock = userRepository.getUserLevelLock(uid)
		if (getLock != 1) {
			throw ServerErrorException(INTERNAL_ERROR_CODE.DB_USER_LEVEL_LOCK_NOT_RELEASE)
		}
		println("getLock: $getLock")
	}

	@After("@annotation(UserLevelLock) && execution(* *(..))")
	fun userLevelLockAfter() {
		val userTokenInfo = authComponent.getUserTokenInfo()
		val uid = userTokenInfo.uid
		val releaseLock = userRepository.releaseUserLevelLock(uid)
		println("releaseLock: $releaseLock")
	}
}
