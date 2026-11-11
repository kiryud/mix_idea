package com.ideasupport.lib.util
import jakarta.persistence.EntityManager
import jakarta.persistence.Table
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/** H2 기반 기존 repository 테스트 전용. 실제 MySQL 테스트는 격리 DB를 사용한다. */
@Component
class DatabaseCleaner(private val entityManager: EntityManager) {
	@Transactional fun clear() {
		entityManager.clear()
		val tables = entityManager.metamodel.entities.mapNotNull { it.javaType.getAnnotation(Table::class.java)?.name }
		entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate()
		try {
			tables.forEach { entityManager.createNativeQuery("TRUNCATE TABLE $it").executeUpdate() }
		} finally {
			entityManager.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate()
		}
	}
}
