pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/bootstrap")
    }
    
}
rootProject.name = "kotlin-compiler-course-examples"

include("compiler-plugin")
include("plugin-annotations")
