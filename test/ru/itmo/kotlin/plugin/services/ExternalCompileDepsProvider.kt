package ru.itmo.kotlin.plugin.services

import org.jetbrains.kotlin.cli.jvm.config.addJvmClasspathRoot
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import java.io.File
import java.io.FilenameFilter

class ExternalCompileDepsProvider(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    companion object {
        internal const val EXTERNAL_JARS_DIR = "testData/jars/"
        internal val COROUTINES_JAR_FILTER = FilenameFilter { _, name -> name.startsWith("kotlinx-coroutines") && name.endsWith(".jar") }
        internal val failMessage = { "Jar file kotlinx.coroutines does not exist" }
    }

    override fun configureCompilerConfiguration(configuration: CompilerConfiguration, module: TestModule) {
        val libDir = File(EXTERNAL_JARS_DIR)
        testServices.assertions.assertTrue(libDir.exists() && libDir.isDirectory, failMessage)
        val jar = libDir.listFiles(COROUTINES_JAR_FILTER)?.firstOrNull() ?: testServices.assertions.fail(failMessage)
        configuration.addJvmClasspathRoot(jar)
    }
}