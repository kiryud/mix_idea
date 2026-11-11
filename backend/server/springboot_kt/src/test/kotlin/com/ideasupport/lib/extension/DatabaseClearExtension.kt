package com.ideasupport.lib.extension

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.springframework.test.context.junit.jupiter.SpringExtension
import com.ideasupport.lib.util.DatabaseCleaner

class DatabaseClearExtension : AfterAllCallback {
	@Throws(Exception::class)
	override fun afterAll(context: ExtensionContext) {
		val databaseCleaner = getDataCleaner(context)
		databaseCleaner.clear()
	}

	private fun getDataCleaner(extensionContext: ExtensionContext): DatabaseCleaner {
		return SpringExtension.getApplicationContext(extensionContext)
			.getBean(DatabaseCleaner::class.java)
	}
}
