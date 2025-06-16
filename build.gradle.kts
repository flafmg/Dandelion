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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("io.netty:netty-all:4.1.24.Final")
}

application {
    mainClass.set("org.dandelion.classic.server.ServerKt")
}

tasks.test {
    useJUnitPlatform()
}