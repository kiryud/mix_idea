package com.ideasupport.infra.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import com.ideasupport.core.entity.UserEntity

@Repository
interface UserRepository : JpaRepository<UserEntity, Long> {
	@Query(value = "SELECT GET_LOCK(?1,3)", nativeQuery = true)
	fun getUserLevelLock(uid: String): Int

	@Query(value = "SELECT RELEASE_LOCK(?1)", nativeQuery = true)
	fun releaseUserLevelLock(uid: String): Int

	fun findByIdx(idx: Long): UserEntity?

	fun findById(id: String): UserEntity?

	fun findByUid(uid: String): UserEntity?

	fun deleteByUid(uid: String): Long
}
