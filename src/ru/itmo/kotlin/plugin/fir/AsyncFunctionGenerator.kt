package ru.itmo.kotlin.plugin.fir

import com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.KtRealPsiSourceElement
import org.jetbrains.kotlin.contracts.description.EventOccurrencesRange
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.getContainingClassSymbol
import org.jetbrains.kotlin.fir.builder.buildLabel
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.builder.FirSimpleFunctionBuilder
import org.jetbrains.kotlin.fir.declarations.builder.buildAnonymousFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyBackingField
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.isSuspend
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.FirResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import ru.itmo.kotlin.plugin.fir.utils.Finder
import ru.itmo.kotlin.plugin.fir.utils.Names
import ru.itmo.kotlin.plugin.fir.utils.Names.COROUTINE_SCOPE_CLASS_ID
import ru.itmo.kotlin.plugin.fir.utils.Names.NOTHING_CLASS_ID
import ru.itmo.kotlin.plugin.fir.utils.Names.SUSPEND_FUNCTION_CLASS_ID
import ru.itmo.kotlin.plugin.fir.utils.Names.toAsync


/**
 * For all `suspend fun funcName(args..): retType` annotated with [ru.itmo.kotlin.plugin.desuspender.DeSuspend]
 * generates new function `fun funcNameAsync(args..): Future<retType>`
 */
class AsyncFunctionGenerator(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val ANNOTATED_SUSPEND_PREDICATE = has(Names.ANNOTATION_FQN)
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedAnnotatedFunctions by lazy {
        predicateBasedProvider.getSymbolsByPredicate(ANNOTATED_SUSPEND_PREDICATE)
            .filterIsInstance<FirNamedFunctionSymbol>()
    }
    private val topLevelAnnotatedFunctions by lazy {
        matchedAnnotatedFunctions.filter { it.containingClass() == null }
    }
    private val memberAnnotatedFunctions by lazy {
        matchedAnnotatedFunctions.filter { it.containingClass() != null }
    }
    private val annotatedMethodsByClassSymbol by lazy {
        memberAnnotatedFunctions.groupBy { it.getContainingClassSymbol(session)!! }
    }
    private val topLevelGeneratedCallableIdsAndOriginals by lazy {
        topLevelAnnotatedFunctions.associateBy { CallableId(it.callableId.packageName, it.name.toAsync()) }
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return annotatedMethodsByClassSymbol[classSymbol]?.map { it.name.toAsync() }?.toSet() ?: emptySet()
    }

    override fun getTopLevelCallableIds(): Set<CallableId> {
        return topLevelAnnotatedFunctions.mapTo(HashSet()) {
            CallableId(it.callableId.packageName, it.callableId.callableName.toAsync())
        } + Names.ASYNC_EXECUTOR_CALLABLE_ID
    }

    override fun generateProperties(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirPropertySymbol> {
        return if (callableId == Names.ASYNC_EXECUTOR_CALLABLE_ID) {
            val propSymbol = FirPropertySymbol(callableId)
            val propInitializer = createFixedThreadPool()
            listOf(
                buildProperty {
                    moduleData = session.moduleData
                    resolvePhase = FirResolvePhase.BODY_RESOLVE
                    origin = Key.origin
                    status = FirResolvedDeclarationStatusImpl.DEFAULT_STATUS_FOR_STATUSLESS_DECLARATIONS
                    returnTypeRef = propInitializer.typeRef
                    name = callableId.callableName
                    initializer = propInitializer
                    isVar = false
                    getter = FirDefaultPropertyGetter(
                        source = null,
                        moduleData = session.moduleData,
                        origin = Key.origin,
                        propertyTypeRef = propInitializer.typeRef,
                        visibility = Visibilities.Public,
                        propertySymbol = propSymbol,
                        effectiveVisibility = EffectiveVisibility.Public,

                    )
                    backingField = FirDefaultPropertyBackingField(
                        moduleData = session.moduleData,
                        annotations = mutableListOf(),
                        returnTypeRef = propInitializer.typeRef,
                        isVar = false,
                        propertySymbol = propSymbol,
                        status = FirResolvedDeclarationStatusImpl(
                            Visibilities.Private,
                            Modality.FINAL,
                            EffectiveVisibility.PrivateInClass)
                    )
                    symbol = propSymbol
                    isLocal = false
                    bodyResolveState = FirPropertyBodyResolveState.EVERYTHING_RESOLVED
                }.symbol
            )
        } else emptyList()
    }

    @OptIn(SymbolInternals::class)
    private fun createFixedThreadPool(): FirFunctionCall {
        return buildFunctionCall {
            typeRef = buildResolvedTypeRef {
                type = Names.EXECUTOR_SERVICE_CLASS_ID.toFlexibleConeClassType()
            }
            explicitReceiver = buildResolvedQualifier {
                typeRef = session.builtinTypes.unitType
                packageFqName = Names.CONCURRENT_PKG_FQN
                relativeClassFqName = FqName("Executors")
                symbol = Finder.findExecutorsSymbol(session)
            }
            calleeReference = buildResolvedNamedReference {
                name = Names.NEW_FIXED_THREAD_POOL_NAME
                resolvedSymbol = Finder.findFixedThreadPoolSymbol(session)
            }
            argumentList = buildResolvedArgumentList(
                LinkedHashMap(
                    mapOf(
                        buildConstExpression(source = null, kind = ConstantValueKind.Int, value = 10).apply {
                            replaceTypeRef(session.builtinTypes.intType)
                        } to Finder.findFixedThreadPoolSymbol(session).fir.valueParameters[0]
                    )
                )
            )
        }
    }

    override fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> {
        return if (owner == null) {
            topLevelGeneratedCallableIdsAndOriginals[callableId]
        } else {
            annotatedMethodsByClassSymbol[owner]?.find {
                CallableId(it.callableId.packageName, it.callableId.className, it.name.toAsync()) == callableId
            }
        }?.takeIf { it.isSuspend }?.let { original ->
            listOf(
                buildSimpleFunction {
                    name = callableId.callableName
                    symbol = FirNamedFunctionSymbol(callableId)
                    generateRestDeclarationExceptBody(original)
                }.apply {
                    replaceBody(generateBody(original, owner))
                }.symbol
            )
        } ?: emptyList()
    }

    @OptIn(SymbolInternals::class)
    private fun FirSimpleFunctionBuilder.generateRestDeclarationExceptBody(original: FirNamedFunctionSymbol) {
        moduleData = session.moduleData
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        origin = Key.origin
        status = FirResolvedDeclarationStatusImpl(
            original.visibility,
            Modality.FINAL,
            original.effectiveVisibility
        )
        returnTypeRef = buildResolvedTypeRef {
            type = Names.FUTURE_CLASS_ID.toConeClassType(arrayOf(original.resolvedReturnType))
        }
        dispatchReceiverType = original.dispatchReceiverType
        valueParameters += original.fir.valueParameters
    }

    private fun FirSimpleFunction.generateBody(original: FirNamedFunctionSymbol, owner: FirClassSymbol<*>?): FirBlock {
        return buildSingleExpressionBlock(
            statement = buildReturnExpression {
                target = FirFunctionTarget(original.callableId.callableName.asString(), isLambda = false).apply {
                    bind(this@generateBody)
                }
                result = buildFunctionCall {
                    val typeArg = original.resolvedReturnType.classId!!.toFlexibleConeClassType()
                    typeRef = buildResolvedTypeRef {
                        type = Names.FUTURE_CLASS_ID.toFlexibleConeClassType(arrayOf(typeArg))
                    }
                    typeArguments += buildTypeProjectionWithVariance {
                        typeRef = buildResolvedTypeRef { type = typeArg }
                        variance = Variance.INVARIANT
                    }
                    val receiver = buildPropertyAccessExpression {
                        typeRef = buildResolvedTypeRef {
                            type = Names.EXECUTOR_SERVICE_CLASS_ID.toFlexibleConeClassType()
                        }
                        calleeReference = buildResolvedNamedReference {
                            name = Names.ASYNC_EXECUTOR_NAME
                            resolvedSymbol = Finder.findInternalThreadPoolSymbol(session)
                        }
                    }
                    explicitReceiver = receiver
                    dispatchReceiver = receiver
                    calleeReference = buildResolvedNamedReference {
                        name = Names.SUBMIT_NAME
                        resolvedSymbol = Finder.findSubmitSymbol(session)
                    }
                    argumentList = this@generateBody.generateArgumentList(original, owner)
                }
            }
        ).apply {
            replaceTypeRef(buildResolvedTypeRef { type = NOTHING_CLASS_ID.toConeClassType() })
        }
    }

    @OptIn(SymbolInternals::class)
    private fun FirSimpleFunction.generateArgumentList(original: FirNamedFunctionSymbol, owner: FirClassSymbol<*>?): FirResolvedArgumentList {
        return buildResolvedArgumentList(
            LinkedHashMap(mapOf(
                buildLambdaArgumentExpression {
                    expression = buildAnonymousFunctionExpression {
                        anonymousFunction = buildAnonymousFunction {
                            // intentionally added to treat anonymous function as lambda
                            source = KtRealPsiSourceElement(KtFunctionLiteral(
                                CompositeElement(KtNodeTypes.FUNCTION_LITERAL)
                            ))
                            moduleData = session.moduleData
                            origin = Key.origin
                            symbol = FirAnonymousFunctionSymbol()
                            inlineStatus = InlineStatus.NoInline
                            label = buildLabel { name = "submit" }
                            isLambda = true
                            hasExplicitParameterList = false
                            val retTypeRef = original.resolvedReturnType.classId!!.toFlexibleConeClassType()
                            returnTypeRef = buildResolvedTypeRef {
                                type = retTypeRef
                            }
                            typeRef = buildResolvedTypeRef {
                                type = Names.FUNCTION0_CLASS_ID.toConeClassType(arrayOf(retTypeRef))
                            }
                        }.also { anonFunc1 ->
                            anonFunc1.replaceBody(buildSingleExpressionBlock(
                                statement = buildReturnExpression {
                                    target = FirFunctionTarget(
                                        labelName = null,
                                        isLambda = true
                                    ).apply {
                                        bind(anonFunc1)
                                    }
                                    result = buildFunctionCall {
                                        typeRef = original.resolvedReturnTypeRef
                                        typeArguments += buildTypeProjectionWithVariance {
                                            typeRef = original.resolvedReturnTypeRef
                                            variance = Variance.INVARIANT
                                        }
                                        calleeReference = buildResolvedNamedReference {
                                            name = Names.RUN_BLOCKING_NAME
                                            resolvedSymbol = Finder.findRunBlockingSymbol(session)
                                        }
                                        argumentList = buildResolvedArgumentList(
                                            LinkedHashMap(mapOf(
                                                buildLambdaArgumentExpression {
                                                    expression = buildAnonymousFunctionExpression {
                                                        anonymousFunction = buildAnonymousFunction {
                                                            // intentionally added to treat anonymous function as lambda
                                                            source = KtRealPsiSourceElement(KtFunctionLiteral(
                                                                CompositeElement(KtNodeTypes.FUNCTION_LITERAL)
                                                            ))
                                                            val runBlockingValParam = Finder.findRunBlockingSymbol(session).fir.valueParameters[1]
                                                            moduleData = session.moduleData
                                                            origin = Key.origin
                                                            returnTypeRef = original.resolvedReturnTypeRef
                                                            receiverTypeRef = buildResolvedTypeRef {
                                                                type = COROUTINE_SCOPE_CLASS_ID.toConeClassType()
                                                            }
                                                            symbol = FirAnonymousFunctionSymbol()
                                                            label = buildLabel { name = "runBlocking" }
                                                            invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                                                            inlineStatus = InlineStatus.NoInline
                                                            isLambda = true
                                                            hasExplicitParameterList = false
                                                            typeRef = buildResolvedTypeRef {
                                                                type = SUSPEND_FUNCTION_CLASS_ID.toConeClassType(arrayOf(
                                                                        runBlockingValParam.returnTypeRef.coneType.typeArguments[0],
                                                                        original.resolvedReturnType
                                                                    ), attributes = ConeAttributes.WithExtensionFunctionType)
                                                            }
                                                        }.also { anonFunc2 ->
                                                            anonFunc2.replaceBody(buildSingleExpressionBlock(
                                                                statement = buildReturnExpression {
                                                                    target = FirFunctionTarget(
                                                                        labelName = null,
                                                                        isLambda = true
                                                                    ).apply {
                                                                        bind(anonFunc2)
                                                                    }
                                                                    result = buildFunctionCall {
                                                                        typeRef = original.resolvedReturnTypeRef
                                                                        dispatchReceiver = owner?.let {
                                                                            buildThisReceiverExpression {
                                                                                typeRef = buildResolvedTypeRef {
                                                                                    type = owner.classId.toConeClassType()
                                                                                }
                                                                                calleeReference = buildImplicitThisReference {
                                                                                    boundSymbol = owner
                                                                                }
                                                                                isImplicit = true
                                                                            }
                                                                        } ?: FirNoReceiverExpression
                                                                        argumentList = buildResolvedArgumentList(LinkedHashMap(
                                                                            mapOf(
                                                                                *this@generateArgumentList.valueParameters.map { param ->
                                                                                    buildPropertyAccessExpression {
                                                                                        typeRef = param.returnTypeRef
                                                                                        calleeReference = buildResolvedNamedReference {
                                                                                            name = param.name
                                                                                            resolvedSymbol = param.symbol
                                                                                        }
                                                                                    } to original.fir.valueParameters.find { it.name == param.name}!!
                                                                                }.toTypedArray()
                                                                            )
                                                                        ))
                                                                        calleeReference =
                                                                            buildResolvedNamedReference {
                                                                                name = original.name
                                                                                resolvedSymbol = original
                                                                            }
                                                                    }
                                                                }
                                                            ).apply {
                                                                replaceTypeRef(original.resolvedReturnTypeRef)
                                                            })
                                                        }
                                                    }
                                                } to Finder.findRunBlockingSymbol(session).fir.valueParameters[1]
                                            ))
                                        )
                                    }
                                }
                            ).apply { replaceTypeRef(original.resolvedReturnTypeRef) }
                            )
                        }
                    }
                } to Finder.findSubmitSymbol(session).fir.valueParameters[0]
            ))
        )
    }

    private fun ClassId.toFlexibleConeClassType(typeArgs: Array<ConeTypeProjection> = emptyArray()): ConeFlexibleType {
        val lookupTag = ConeClassLikeLookupTagImpl(this)
        return ConeFlexibleType(
            lowerBound = ConeClassLikeTypeImpl(lookupTag, typeArgs, isNullable = false),
            upperBound = ConeClassLikeTypeImpl(lookupTag, typeArgs, isNullable = true),
        )
    }

    private fun ClassId.toConeClassType(typeArgs: Array<ConeTypeProjection> = emptyArray(), isNullable: Boolean = false,
                                        attributes: ConeAttributes = ConeAttributes.Empty): ConeClassLikeType {
        val lookupTag = ConeClassLikeLookupTagImpl(this)
        return ConeClassLikeTypeImpl(lookupTag, typeArgs, isNullable, attributes = attributes)
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(ANNOTATED_SUSPEND_PREDICATE)
    }

    object Key : FirPluginKey() {
        override fun toString(): String {
            return "DeSuspender"
        }
    }
}