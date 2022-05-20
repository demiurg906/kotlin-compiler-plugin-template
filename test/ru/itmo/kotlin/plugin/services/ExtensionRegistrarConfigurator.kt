package ru.itmo.kotlin.plugin.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import ru.itmo.kotlin.plugin.DeSuspendPluginRegistrar

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun registerCompilerExtensions(
        project: Project,
        module: TestModule,
        configuration: CompilerConfiguration
    ) {
        FirExtensionRegistrar.registerExtension(project, DeSuspendPluginRegistrar())
    }
}
