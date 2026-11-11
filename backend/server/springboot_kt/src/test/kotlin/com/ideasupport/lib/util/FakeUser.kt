package com.ideasupport.lib.util
import java.util.UUID
import com.ideasupport.core.entity.UserEntity

class FakeUser {
	fun makeFakeUser() = UserEntity(uid = UUID.randomUUID().toString(), email = "student@example.com", name = "학생")
}
