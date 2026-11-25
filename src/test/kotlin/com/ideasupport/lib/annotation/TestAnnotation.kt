package com.ideasupport.lib.annotation

import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit.jupiter.SpringExtension
import com.ideasupport.lib.common.objects.Profiles
import com.ideasupport.lib.extension.DatabaseClearExtension

@ActiveProfiles(Profiles.TEST)
@ExtendWith(SpringExtension::class)
@ExtendWith(DatabaseClearExtension::class)
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
annotation class LargeTest

@ComponentScan(basePackages = ["com.ideasupport.lib.util", "com.ideasupport.lib.mock"])
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ExtendWith(DatabaseClearExtension::class)
@ActiveProfiles(Profiles.TEST)
@DataJpaTest(excludeAutoConfiguration = [LiquibaseAutoConfiguration::class])
annotation class MediumTest

@ComponentScan(basePackages = ["com.ideasupport.infra.config"])
@ActiveProfiles(Profiles.TEST)
@ExtendWith(DatabaseClearExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
annotation class SmallTest
