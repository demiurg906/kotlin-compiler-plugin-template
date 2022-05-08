/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.fir.backend.IrPluginDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrBody
import org.jetbrains.kotlin.ir.expressions.IrFieldAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrMemberAccessExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.isPropertyAccessor
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

abstract class AbstractTransformerForGenerator(protected val context: IrPluginContext) : IrElementVisitorVoid {
    protected val irFactory = context.irFactory
    protected val irBuiltIns = context.irBuiltIns

    abstract fun interestedIn(key: FirPluginKey): Boolean

    final override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitMemberAccess(expression: IrMemberAccessExpression<*>, data: Nothing?) {
        TODO("fff")
    }

    override fun visitFieldAccess(expression: IrFieldAccessExpression) {
        TODO("vvv")
    }

    final override fun visitProperty(declaration: IrProperty) {
        val origin = declaration.origin
        if (origin !is IrPluginDeclarationOrigin || !interestedIn(origin.pluginKey)) return
        val getter = declaration.getter
        require(getter != null)
        println(">>>>>>>")
        val delegatingAnyCall = IrDelegatingConstructorCallImpl(
            -1,
            -1,
            irBuiltIns.anyType,
            irBuiltIns.anyClass.owner.primaryConstructor?.symbol ?: return,
            typeArgumentsCount = 0,
            valueArgumentsCount = 0
        )
        val constructor = getter.returnType.getClass()?.primaryConstructor ?: throw Exception("Wrong DI variable type")
        val constructorCall = IrConstructorCallImpl(
            -1,
            -1,
            getter.returnType,
            constructor.symbol,
            typeArgumentsCount = 0,
            constructorTypeArgumentsCount = 0,
            valueArgumentsCount = 0
        )
        val returnStatement = IrReturnImpl(-1, -1, getter.returnType, getter.symbol, constructorCall)
        getter.body = irFactory.createBlockBody(-1, -1, listOf(returnStatement))
    }

//    final override fun visitVariable(declaration: IrVariable) {
//        super.visitVariable(declaration)
//        val origin = declaration.origin
//        println("xxxx")
//        if (origin !is IrPluginDeclarationOrigin || !interestedIn(origin.pluginKey)) return
//        require(declaration.initializer == null)
////        val constructor = declaration.type.getClass()?.primaryConstructor ?: throw Exception("Wrong DI variable type")
////        val constructorCall = IrConstructorCallImpl(
////            -1,
////            -1,
////            declaration.type,
////            constructor.symbol,
////            typeArgumentsCount = 0,
////            constructorTypeArgumentsCount = 0,
////            valueArgumentsCount = 0
////        )
//        declaration.initializer = TODO("xxx")
//    }
}
