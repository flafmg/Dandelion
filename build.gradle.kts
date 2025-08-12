plugins {
    kotlin("jvm") version "2.1.0"
    application
}
kotlin {
    jvmToolchain(21)
}

group = "org.dandelion.classic"
version = "0.1.1-dev"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("com.google.code.gson:gson:2.13.1")
    implementation("org.yaml:snakeyaml:2.4")
    implementation("io.netty:netty-all:4.2.2.Final")
    implementation("org.jline:jline:3.30.0")
}

application {
    mainClass.set("org.dandelion.classic.server.ServerKt")
}

tasks.test {
    useJUnitPlatform()
}
tasks.jar {
    manifest {
        attributes["Main-Class"] = application.mainClass.get()
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
}