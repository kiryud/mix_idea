package com.ideasupport.main.app.user
import org.springframework.stereotype.Component
import com.ideasupport.core.entity.UserEntity
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.lib.common.component.AuthComponent
import com.ideasupport.lib.common.enum.INTERNAL_ERROR_CODE
import com.ideasupport.lib.common.exception.ServerErrorException

@Component
class UserIdentityComponent(private val authComponent: AuthComponent, private val users: UserRepository) {
	fun current(): UserEntity =
		users.findByUid(authComponent.getUserTokenInfo().uid) ?: throw ServerErrorException(INTERNAL_ERROR_CODE.INVALID_TOKEN)
}
