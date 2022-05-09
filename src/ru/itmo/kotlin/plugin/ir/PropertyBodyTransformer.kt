/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.fir.backend.IrPluginDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrFieldSymbolImpl
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.fir.DependencyInjector

class PropertyBodyTransformer(context: IrPluginContext) : IrElementVisitorVoid {
    private val irFactory = context.irFactory
    private val injected = mutableMapOf<String, IrField>()

    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitProperty(declaration: IrProperty) {
        val origin = declaration.origin
        val name = declaration.name.asString()
        val getter = declaration.getter
        if (origin !is IrPluginDeclarationOrigin || !interestedIn(origin.pluginKey)) return
        require(getter != null)

        injected[name]?.let { it ->
            val getField = IrGetFieldImpl(-1, -1, it.symbol, it.type)
            getter.body = irFactory.createBlockBody(
                -1, -1, listOf(
                    IrReturnImpl(
                        -1, -1, getter.returnType, getter.symbol,
                        getField
                    )
                )
            )
            return
        }
        val constructor = getter.returnType.getClass()?.primaryConstructor ?: throw Exception("Wrong DI variable type")
        val constructorCall = IrConstructorCallImpl(
            -1, -1, getter.returnType, constructor.symbol, typeArgumentsCount = 0, constructorTypeArgumentsCount = 0, valueArgumentsCount = 0
        )

        val variable = IrFieldImpl(
            -1,
            -1,
            declaration.origin,
            IrFieldSymbolImpl(),
            Name.identifier("$${declaration.backingField?.name?.asString()}"),
            getter.returnType,
            DescriptorVisibilities.DEFAULT_VISIBILITY,
            false,
            false,
            true
        )
        variable.parent = declaration.parent
        injected[name] = variable
        variable.initializer = irFactory.createExpressionBody(constructorCall)
        declaration.backingField = variable
        getter.body = irFactory.createBlockBody(
            -1,
            -1,
            listOf(IrReturnImpl(-1, -1, getter.returnType, getter.symbol, IrGetFieldImpl(-1, -1, variable.symbol, variable.type)))
        )
    }

    private fun interestedIn(key: FirPluginKey): Boolean {
        return key == DependencyInjector.Key
    }
}
