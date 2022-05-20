package ru.itmo.kotlin.plugin.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.backend.IrPluginDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildReceiverParameter
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetFieldImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrReturnImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWithParameters
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.fir.DependencyInjector

class PropertyBodyTransformer(context: IrPluginContext) : IrElementVisitorVoid {
    private val irFactory = context.irFactory
    private val irBuiltIns = context.irBuiltIns
    private val injected = mutableMapOf<String, IrField>()
    private val singletonLazy = lazy {
        irFactory.buildClass {
            kind = ClassKind.OBJECT
            name = Name.identifier("\$DIClass")
        }.apply {
            thisReceiver = buildReceiverParameter(this, IrDeclarationOrigin.INSTANCE_RECEIVER, symbol.typeWithParameters(typeParameters))
            addConstructor {
                isPrimary = true
            }.apply {
                body = irFactory.createBlockBody(
                    -1, -1, listOf(
                        IrDelegatingConstructorCallImpl(
                            -1, -1, irBuiltIns.anyType, irBuiltIns.anyClass.owner.constructors.single().symbol, 0, 0
                        )
                    )
                )
            }
        }
    }
    private val singleton by singletonLazy

    private var isSingletonBound = false
    override fun visitElement(element: IrElement) {
        when (element) {
            is IrDeclaration, is IrFile, is IrModuleFragment -> element.acceptChildrenVoid(this)
            else -> {}
        }
        if (element is IrFile && singletonLazy.isInitialized() && !isSingletonBound) {
            element.bind(singleton)
            isSingletonBound = true
        }
    }

    private fun <T : IrDeclarationContainer> T.bind(declaration: IrDeclaration) = apply {
        declarations.add(declaration)
        declaration.parent = this
    }

    private fun fieldGetter(symbol: IrSimpleFunctionSymbol, field: IrField) = irFactory.createBlockBody(
        -1, -1, listOf(
            IrReturnImpl(
                -1, -1, field.type, symbol, IrGetFieldImpl(-1, -1, field.symbol, field.type, origin = IrStatementOrigin.GET_PROPERTY)
            )
        )
    )

    override fun visitProperty(declaration: IrProperty) {
        val origin = declaration.origin

        val name = declaration.name.asString()
        val getter = declaration.getter
        if (origin !is IrPluginDeclarationOrigin || !interestedIn(origin.pluginKey)) return
        require(getter != null)
        val type = getter.returnType

        injected[name]?.let { it ->
            getter.body = fieldGetter(getter.symbol, it)
            return
        }
        val field = irFactory.buildField {
            this.type = type
            this.isStatic = true
            this.name = declaration.name
        }
        singleton.bind(field)
        val constructor = type.getClass()?.primaryConstructor ?: throw Exception("Wrong DI variable type")
        val constructorCall = IrConstructorCallImpl(
            -1, -1, type, constructor.symbol, typeArgumentsCount = 0, constructorTypeArgumentsCount = 0, valueArgumentsCount = 0
        )
        field.initializer = irFactory.createExpressionBody(constructorCall)

        injected[name] = field
        getter.body = fieldGetter(getter.symbol, field)
    }

    private fun interestedIn(key: FirPluginKey): Boolean {
        return key == DependencyInjector.Key
    }
}
