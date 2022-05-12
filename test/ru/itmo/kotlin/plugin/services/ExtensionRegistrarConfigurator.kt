package ru.itmo.kotlin.plugin.services

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices
import ru.itmo.kotlin.plugin.SimplePluginRegistrar
import ru.itmo.kotlin.plugin.ir.SimpleIrGenerationExtension

class ExtensionRegistrarConfigurator(testServices: TestServices) : EnvironmentConfigurator(testServices) {
    override fun registerCompilerExtensions(project: Project, module: TestModule, configuration: CompilerConfiguration) {
        FirExtensionRegistrar.registerExtension(project, SimplePluginRegistrar())
        IrGenerationExtension.registerExtension(project, SimpleIrGenerationExtension())
    }
}
