package com.ideasupport.infra.repository
import com.ideasupport.core.entity.MatchCacheEntity
import org.springframework.data.jpa.repository.*

interface MatchCacheRepository : JpaRepository<MatchCacheEntity, String>
