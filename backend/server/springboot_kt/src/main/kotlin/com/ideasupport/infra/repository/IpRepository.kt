package com.ideasupport.infra.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import com.ideasupport.core.entity.IpEntity
import com.ideasupport.core.entity.IpRange
import com.ideasupport.core.entity.IpType

@Repository
interface IpRepository : JpaRepository<IpEntity, Long> {
	fun findByIpTypeAndIpRange(
		ipType: IpType,
		ipRange: IpRange,
	): List<IpEntity>
}
