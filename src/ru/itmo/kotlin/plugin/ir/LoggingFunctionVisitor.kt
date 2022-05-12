package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.allOverridden
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler.signatureString
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.kotlinFqName
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class LoggingFunctionVisitor(private val context: IrPluginContext) : IrElementVisitorVoid {

    companion object {
        val ANNOTATION_CLASS_NAME = FqName("org.itmo.my.pretty.plugin.LogActualClass")
        val KOTLIN_PRINTLN_NAME = Name.identifier("println")
        val KOTLIN_IO_FQ_NAME = FqName("kotlin.io")
    }

    private val printlnSymbol by lazy {
        context.irBuiltIns.findFunctions(KOTLIN_PRINTLN_NAME, KOTLIN_IO_FQ_NAME)
            .find {
                val params = it.owner.valueParameters
                params.size == 1 && params[0].type == context.irBuiltIns.anyNType
            }!!
    }
    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration,
            is IrFile,
            is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
    }

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        val annotated = isFunctionAnnotated(declaration)
        val superAnnotated = declaration.allOverridden().any { isFunctionAnnotated(it) }
        if (!annotated && !superAnnotated) return

        (declaration.body as? IrBlockBodyImpl)?.let {
            val logStringValue =
                "${declaration.kotlinFqName.parent()}." +
                        "${declaration.signatureString(false)} has been called"

            val logString = IrConstImpl(-1, -1, context.irBuiltIns.stringType,
                IrConstKind.String, value = logStringValue)


            val logCall = IrCallImpl(-1, -1, context.irBuiltIns.unitType, printlnSymbol, 0, 1).apply {
                putValueArgument(0, logString)
            }

            it.statements.add(0, logCall)
        }

        super.visitSimpleFunction(declaration)
    }

    private fun isFunctionAnnotated(declaration: IrSimpleFunction) =
        declaration.annotations.any { it.type.classFqName == ANNOTATION_CLASS_NAME }
}