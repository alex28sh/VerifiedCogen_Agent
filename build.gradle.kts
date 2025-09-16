plugins {
    id("org.jetbrains.kotlin.jvm") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("application")
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven { url = uri("https://repo.gradle.org/gradle/libs-releases") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public/") }
}

dependencies {
    // Koog agents library
    implementation("ai.koog:koog-agents:0.4.0")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
//
    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
//    implementation("ch.qos.logback:logback-classic:1.4.11") // or latest
//    // File operations and utilities
//    implementation("commons-io:commons-io:2.13.0")
//
//    // Gradle tooling API for running Gradle builds
////    implementation("org.gradle:gradle-tooling-api:8.4")
//
//    // Spring framework for testing
//    implementation("org.springframework:spring-context:6.0.19")


//    implementation("org.gradle:gradle-tooling-api:${toolingApiVersion}")
    // The tooling API need an SLF4J implementation available at runtime, replace this with any other implementation
    runtimeOnly("org.slf4j:slf4j-simple:2.0.17")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")

    implementation("me.tongfei:progressbar:0.9.5")
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")

    implementation("ai.grazie.api:api-gateway-client-jvm:0.3.168")
    implementation("ai.grazie.client:client-ktor-jvm:0.3.168")
    implementation("ai.jetbrains.code.prompt:code-prompt-executor-grazie-koog-jvm:1.0.0-beta.134")
    implementation("ai.jetbrains.code.prompt:code-prompt-llm:1.0.0-beta.134")

    implementation("org.jetbrains.kotlinx:kotlinx-cli:0.3.6")
    implementation("black.ninia:jep:4.2.0") // check latest version
//    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}