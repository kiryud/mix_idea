import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import groovy.json.JsonSlurper

plugins {
	id("org.springframework.boot") version "3.2.1"
	id("io.spring.dependency-management") version "1.1.4"
	id("com.google.cloud.tools.jib") version "3.3.2"
	kotlin("jvm") version "1.9.21"
	kotlin("plugin.spring") version "1.9.21"
	kotlin("plugin.jpa") version "1.9.21"
	id("org.jlleitschuh.gradle.ktlint").version("12.1.1")
	jacoco
}

val imageTargetEnv =
	if (properties.containsKey("projectDataImageTargetEnv")) {
		property("projectDataImageTargetEnv")
	} else {
		"local"
	}

group = "com.ideasupport"
version = "0.0.1-SNAPSHOT"

java {
	sourceCompatibility = JavaVersion.VERSION_17
}

repositories {
	mavenCentral()
}

dependencies {
	// Spring Setting
	implementation("org.springframework.boot:spring-boot-starter")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-aop")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.security:spring-security-test")
	testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")

	// Database
	runtimeOnly("com.mysql:mysql-connector-j")
	testRuntimeOnly("com.h2database:h2")
	implementation("com.vladmihalcea:hibernate-types-60:2.20.0")

	// Util
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")
	implementation("io.github.microutils:kotlin-logging:3.0.5")
	compileOnly("ch.qos.logback.contrib:logback-jackson:0.1.5")
	compileOnly("ch.qos.logback.contrib:logback-json-classic:0.1.5")
	implementation("net.datafaker:datafaker:2.0.2")
	implementation("com.google.code.gson:gson:2.8.5")
	implementation("org.zeroturnaround:zt-zip:1.8")
	implementation("net.logstash.logback:logstash-logback-encoder:7.4")
	implementation("io.jsonwebtoken:jjwt-api:0.11.5")
	implementation("io.jsonwebtoken:jjwt-impl:0.11.5")
	implementation("io.jsonwebtoken:jjwt-jackson:0.11.5")
	implementation("org.json:json:20231013")
	implementation("org.liquibase:liquibase-core")
}

tasks.withType<KotlinCompile> {
	kotlinOptions {
		freeCompilerArgs += "-Xjsr305=strict"
		jvmTarget = "17"
	}
}

val prepareTestDatabase by tasks.registering(Exec::class) {
	group = "verification"
	description = "Starts this workspace's isolated MySQL test server on Windows."
	onlyIf { System.getenv("IDEA_TEST_JDBC_URL").isNullOrBlank() && System.getProperty("os.name").startsWith("Windows") }
	commandLine("pwsh", "-NoProfile", "-File", file("scripts/prepare-test-db.ps1").absolutePath)
}

tasks.withType<Test>().configureEach {
	useJUnitPlatform()
	// A build must run tests again and show results, including when production code is unchanged.
	outputs.upToDateWhen { false }
	ignoreFailures = false
	failFast = false
	systemProperty("spring.config.import", "")
	if (System.getProperty("os.name").startsWith("Windows")) {
		val testTemp = File(System.getenv("SystemRoot"), "Temp").absolutePath
		jvmArgs("-Djdk.net.unixdomain.tmpdir=$testTemp", "-Djava.io.tmpdir=$testTemp")
	}
	testLogging {
		events("passed", "failed", "skipped")
		exceptionFormat = TestExceptionFormat.FULL
		showStandardStreams = false
	}
	reports {
		html.required.set(true)
		junitXml.required.set(true)
	}
	addTestListener(
		object : TestListener {
			override fun beforeSuite(suite: TestDescriptor) {}

			override fun beforeTest(testDescriptor: TestDescriptor) {}

			override fun afterTest(
				testDescriptor: TestDescriptor,
				result: TestResult,
			) {}

			override fun afterSuite(
				suite: TestDescriptor,
				result: TestResult,
			) {
				if (suite.parent == null) {
					logger.lifecycle(
						"TEST RESULT: ${result.resultType} | total=${result.testCount} passed=${result.successfulTestCount} failed=${result.failedTestCount} skipped=${result.skippedTestCount}",
					)
					logger.lifecycle("HTML report: ${reports.html.outputLocation.get().asFile}")
				}
			}
		},
	)
}

tasks.test {
	dependsOn(prepareTestDatabase)
	doFirst {
		if (System.getenv("IDEA_TEST_JDBC_URL").isNullOrBlank()) {
			val credentials = file(".local/mysql-test-access.json")
			check(credentials.exists()) {
				"Set IDEA_TEST_JDBC_URL, IDEA_TEST_DB_USER and IDEA_TEST_DB_PASSWORD to a dedicated MySQL database ending in _test. See docs/TESTING_AND_DEMO.md."
			}
			val access = JsonSlurper().parse(credentials) as Map<*, *>
			environment(
				"IDEA_TEST_JDBC_URL",
				"jdbc:mysql://127.0.0.1:33307/idea_support_test?connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true",
			)
			environment("IDEA_TEST_DB_USER", access["user"].toString())
			environment("IDEA_TEST_DB_PASSWORD", access["password"].toString())
		}
		check(environment["IDEA_TEST_JDBC_URL"].toString().matches(Regex("jdbc:mysql://[^/]+/[A-Za-z0-9_]+_test(?:\\?.*)?"))) {
			"Tests require a dedicated MySQL database whose name ends in _test."
		}
	}
	finalizedBy(tasks.jacocoTestReport)
}

jacoco { toolVersion = "0.8.12" }
tasks.jacocoTestReport {
	reports {
		html.required.set(true)
		xml.required.set(true)
		csv.required.set(false)
	}
}

tasks.jacocoTestCoverageVerification {
	dependsOn(tasks.test)
	classDirectories.setFrom(
		sourceSets.main.get().output.asFileTree.matching {
			include("com/ideasupport/main/app/**")
			exclude("com/ideasupport/main/app/dev/**")
		},
	)
	violationRules {
		rule {
			limit {
				counter = "LINE"
				minimum = "0.95".toBigDecimal()
			}
			limit {
				counter = "BRANCH"
				minimum = "0.80".toBigDecimal()
			}
		}
	}
}

val npmCommand = if (System.getProperty("os.name").startsWith("Windows")) "npm.cmd" else "npm"
val demoInstall by tasks.registering(Exec::class) {
	group = "build"
	description = "Installs the demo's exact locked dependencies."
	workingDir("demo-web")
	commandLine(npmCommand, "ci", "--no-audit", "--no-fund", "--cache", file(".local/npm-cache").absolutePath)
	inputs.files("demo-web/package.json", "demo-web/package-lock.json")
	outputs.file("demo-web/node_modules/.package-lock.json")
}
val demoBuild by tasks.registering(Exec::class) {
	group = "verification"
	description = "Runs React tests, fails on errors, and builds the separate demo."
	dependsOn(demoInstall)
	workingDir("demo-web")
	commandLine(npmCommand, "run", "build")
}
tasks.check { dependsOn(demoBuild, tasks.jacocoTestCoverageVerification) }

// 실제 환경 파일은 소스 폴더에서만 읽고 JAR/컨테이너에는 포함하지 않는다.
tasks.processResources {
	exclude(".env", "**/.env")
}

jib {
	from {
		image = "ghcr.io/graalvm/jdk:java17"
		platforms {
			platform {
				architecture = "amd64"
				os = "linux"
			}
			platform {
				architecture = "arm64"
				os = "linux"
			}
		}
	}
	to {
		image =
			providers.gradleProperty("containerImage")
				.orElse(providers.environmentVariable("CONTAINER_IMAGE"))
				.getOrElse("local/idea-support-server")
	}
	container {
		jvmFlags =
			when (imageTargetEnv) {
				"prod" -> listOf("-XX:+UseContainerSupport", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=prod")
				"dev" -> listOf("-XX:+UseContainerSupport", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=dev")
				else -> listOf("-XX:+UseContainerSupport", "-Dfile.encoding=UTF-8", "-Dspring.profiles.active=local")
			}
		ports = listOf("8080", "80", "8088", "9090")
	}
}
