plugins {
    kotlin("jvm")
    id("io.ktor.plugin") version "2.3.9"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
}
val logback_version: String by project

group = "top.kagg886.maimai"
version = "0.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass.set("top.kagg886.maimai.weixin.MainKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.9")
    implementation("io.ktor:ktor-client-cio-jvm:2.3.9")
    testImplementation(kotlin("test"))
    implementation("io.ktor:ktor-client-core")
    implementation("io.ktor:ktor-client-content-negotiation")
    implementation("io.ktor:ktor-serialization-kotlinx-json")
    implementation("io.ktor:ktor-client-logging")
    implementation("org.json:json:20240303")
    implementation("io.ktor:ktor-client-cio")

    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("ch.qos.logback:logback-classic:$logback_version")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}