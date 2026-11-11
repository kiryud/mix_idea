package com.ideasupport.infra.repository
import com.ideasupport.core.entity.RecipeEntity
import org.springframework.data.jpa.repository.*

interface RecipeRepository : JpaRepository<RecipeEntity, String>
