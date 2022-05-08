package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import ru.itmo.kotlin.plugin.fir.DependencyInjector

class SimpleIrBodyGenerator(pluginContext: IrPluginContext) : AbstractTransformerForGenerator(pluginContext) {
    override fun interestedIn(key: FirPluginKey): Boolean {
        return key == DependencyInjector.Key
    }
}
