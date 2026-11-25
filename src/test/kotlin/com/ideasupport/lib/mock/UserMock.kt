package com.ideasupport.lib.mock

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import com.ideasupport.core.entity.UserEntity
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.lib.util.FakeUser

@Component
class UserMock(
	@Autowired private val userRepository: UserRepository,
) {
	/**
	 * [UserMock]
	 * - 유저 생성
	 */
	fun makeMockUser(count: Int): List<UserEntity> {
		for (i in 1..count) {
			userRepository.save(
				FakeUser().makeFakeUser(),
			)
		}
		return userRepository.findAll()
	}
}
