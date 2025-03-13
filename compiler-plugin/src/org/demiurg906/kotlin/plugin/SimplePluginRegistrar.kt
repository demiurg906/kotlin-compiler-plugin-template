package org.demiurg906.kotlin.plugin

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.demiurg906.kotlin.plugin.fir.SimpleClassGenerator

class SimplePluginRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::SimpleClassGenerator
    }
}
