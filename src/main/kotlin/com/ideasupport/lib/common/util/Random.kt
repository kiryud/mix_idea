package com.ideasupport.lib.common.util

import java.util.*
import java.util.Random

class Random {
	fun generateUniqueValue(num: Int): String {
		val uniqueID = UUID.randomUUID().toString()

		return uniqueID.replace("-", "").substring(0, num) // 처음 8자리만 사용
	}

	fun generateRandomBytes(length: Int): String {
		val byteArray = ByteArray(length / 2)
		Random().nextBytes(byteArray)

		return byteArray.joinToString("") { "%02x".format(it) }
	}
}
