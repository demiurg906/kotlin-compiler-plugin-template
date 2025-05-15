plugins {
    kotlin("multiplatform") version "2.0.0" apply false
    kotlin("jvm") version "2.0.0" apply false
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.16.3" apply false
}

allprojects {
    group = "org.demiurg906.kotlin.plugin"
    version = "0.1"
}
