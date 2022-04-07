package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import ru.itmo.kotlin.plugin.fir.SimpleClassGenerator

class SimplePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SimpleClassGenerator
    }
}
