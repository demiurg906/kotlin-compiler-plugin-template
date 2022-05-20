package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import ru.itmo.kotlin.plugin.fir.AsyncFunctionGenerator
import ru.itmo.kotlin.plugin.fir.PluginAdditionalCheckers

class DeSuspendPluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::AsyncFunctionGenerator
        +::PluginAdditionalCheckers
    }
}
