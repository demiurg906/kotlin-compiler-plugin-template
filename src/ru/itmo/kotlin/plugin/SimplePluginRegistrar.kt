package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import ru.itmo.kotlin.plugin.fir.ArrayDeclarationGenerator
import ru.itmo.kotlin.plugin.fir.DependencyInjector
import ru.itmo.kotlin.plugin.fir.PluginAdditionalCheckers

class SimplePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::PluginAdditionalCheckers
        +::DependencyInjector
        +::ArrayDeclarationGenerator
    }
}
