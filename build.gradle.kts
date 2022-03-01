//import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    java
    kotlin("jvm") version "1.6.10"
    application
}
group = "me.demiurg"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

sourceSets {
    main {
        java.setSrcDirs(listOf("src"))
        resources.setSrcDirs(listOf("resources"))
    }
    test {
        java.setSrcDirs(listOf("test"))
        resources.setSrcDirs(listOf("testResources"))
    }
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.8.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClassName = "MainKt"
}