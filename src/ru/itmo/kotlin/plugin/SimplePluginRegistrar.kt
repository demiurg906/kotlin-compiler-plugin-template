package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import ru.itmo.kotlin.plugin.fir.DependencyInjector

class SimplePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::DependencyInjector
    }
}
