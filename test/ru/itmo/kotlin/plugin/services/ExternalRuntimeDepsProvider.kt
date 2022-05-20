package ru.itmo.kotlin.plugin.services

import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.RuntimeClasspathProvider
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import ru.itmo.kotlin.plugin.services.ExternalCompileDepsProvider.Companion.failMessage
import java.io.File

class ExternalRuntimeDepsProvider(testServices: TestServices) : RuntimeClasspathProvider(testServices) {

    override fun runtimeClassPaths(module: TestModule): List<File> {
        val libDir = File(ExternalCompileDepsProvider.EXTERNAL_JARS_DIR)
        testServices.assertions.assertTrue(libDir.exists() && libDir.isDirectory, failMessage)
        val jar = libDir.listFiles(ExternalCompileDepsProvider.COROUTINES_JAR_FILTER)?.firstOrNull()
            ?: testServices.assertions.fail(failMessage)
        return listOf(jar)
    }
}