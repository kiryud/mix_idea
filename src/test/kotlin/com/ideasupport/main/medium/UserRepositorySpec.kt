package com.ideasupport.main.medium

import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import com.ideasupport.infra.repository.UserRepository
import com.ideasupport.lib.annotation.MediumTest
import com.ideasupport.lib.util.FakeUser

@MediumTest
class UserRepositorySpec(
	@Autowired
	private val userRepository: UserRepository,
) {
	val fakeUserEntity = FakeUser().makeFakeUser()

	@Test
	@Order(1)
	fun `User 데이터 저장`() {
		// Users Create
		// given
		val userEntity = fakeUserEntity

		// when
		val userData = userRepository.save(userEntity)

		// then
		Assertions.assertEquals(userEntity, userData)
	}

	@Test
	@Order(2)
	fun `User 데이터 확인`() {
		// Users Read
		// given
		val userEntity = fakeUserEntity
		val savedUserData = userRepository.save(userEntity)

		// when
		val idx = savedUserData.idx

		// then
		val retrievedUserData = userRepository.findByIdx(idx!!)
		Assertions.assertNotNull(retrievedUserData)
		Assertions.assertEquals(userEntity, retrievedUserData)
	}

	@Test
	@Order(3)
	fun `User 데이터 수정`() {
		// Users Update
		// given
		val newFakeUserEntity = FakeUser().makeFakeUser()

		// when
		val savedUserData = userRepository.save(fakeUserEntity)
		savedUserData.name = newFakeUserEntity.name
		val updatedUserData = userRepository.save(savedUserData)

		// then
		Assertions.assertEquals(savedUserData, updatedUserData)
	}

	@Test
	@Order(4)
	fun `User 데이터 삭제`() {
		// Users Delete
		// given
		val userEntity = fakeUserEntity
		val savedUserData = userRepository.save(userEntity)
		val deleteIdx = savedUserData.idx

		// when
		userRepository.deleteById(deleteIdx!!)

		// then
		Assertions.assertNull(userRepository.findByIdx(deleteIdx))
	}
}
