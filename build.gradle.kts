plugins {
    kotlin("jvm") version "2.1.0"
    application
}

group = "org.dandelion"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("io.netty:netty-all:4.2.2.Final")
    implementation("org.jline:jline:3.26.0")
}

application {
    mainClass.set("org.dandelion.classic.server.ServerKt")
}

tasks.test {
    useJUnitPlatform()
}