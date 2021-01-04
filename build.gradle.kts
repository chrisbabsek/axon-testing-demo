import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.3.4.RELEASE"
    id("io.spring.dependency-management") version "1.0.10.RELEASE"
    kotlin("jvm") version "1.4.21"
    kotlin("plugin.spring") version "1.4.21-2"
    kotlin("plugin.jpa") version "1.4.21-2"
}

repositories {
    mavenCentral()
}

group = "de.babsek.demo.axontesting"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("com.h2database:h2")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("io.github.microutils:kotlin-logging:2.0.3")

    implementation("org.axonframework:axon-spring-boot-starter:4.4.5")
    implementation("org.axonframework.extensions.kotlin:axon-kotlin:0.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("com.ninja-squad:springmockk:2.0.3")
    testImplementation("org.testcontainers:junit-jupiter:1.15.0")
    testImplementation("org.axonframework:axon-test:4.4.5")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
        languageVersion = "1.4"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
