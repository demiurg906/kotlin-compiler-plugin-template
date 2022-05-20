package ru.itmo.kotlin.plugin.fir

import com.intellij.psi.impl.source.tree.CompositeElement
import org.jetbrains.kotlin.KtFakeSourceElement
import org.jetbrains.kotlin.KtNodeType
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
import org.jetbrains.kotlin.fir.declarations.builder.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.effectiveVisibility
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.*
import org.jetbrains.kotlin.fir.expressions.impl.FirNoReceiverExpression
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.references.builder.buildImplicitThisReference
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.resolve.SessionHolder
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.scopeSessionKey
import org.jetbrains.kotlin.fir.scopes.getFunctions
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildTypeProjectionWithVariance
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtFunctionLiteral
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.stubs.KotlinFunctionStub
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFunctionStubImpl
import org.jetbrains.kotlin.types.ConstantValueKind
import org.jetbrains.kotlin.types.Variance
import ru.itmo.kotlin.plugin.fir.Names.toAsync


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
        return annotatedMethodsByClassSymbol[classSymbol]?.mapTo(HashSet()) { it.name.toAsync() } ?: emptySet()
    }

    override fun getTopLevelCallableIds(): Set<CallableId> {
        return topLevelAnnotatedFunctions.mapTo(HashSet()) {
            CallableId(it.callableId.packageName, it.callableId.callableName.toAsync())
        }
//        + Names.ASYNC_EXECUTOR_CALLABLE_ID
    }

    override fun generateFunctions(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirNamedFunctionSymbol> {
        return if (owner == null) {
            topLevelGeneratedCallableIdsAndOriginals[callableId]
        } else {
            annotatedMethodsByClassSymbol[owner]?.find {
                CallableId(
                    it.callableId.packageName,
                    it.callableId.className,
                    it.name.toAsync()
                ) == callableId
            }
        }?.let { original ->
            listOf(
                buildSimpleFunction {
                    name = callableId.callableName
                    symbol = FirNamedFunctionSymbol(callableId)
                    generateRestDeclarationExceptBody(original)
                }.apply {
                    symbol.bind(this)
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

    @OptIn(SymbolInternals::class)
    private fun FirSimpleFunction.generateBody(original: FirNamedFunctionSymbol, owner: FirClassSymbol<*>?): FirBlock {
        return buildSingleExpressionBlock(
            statement = buildReturnExpression {
                target = FirFunctionTarget(original.callableId.callableName.asString(), isLambda = false).apply {
                    bind(this@generateBody)
                }
                result = buildFunctionCall {
                    typeRef = buildResolvedTypeRef {
                        type = Names.FUTURE_CLASS_ID.toFlexibleConeClassType(arrayOf(original.resolvedReturnType.classId!!.toFlexibleConeClassType()))
                    }
                    typeArguments += buildTypeProjectionWithVariance {
                        typeRef = buildResolvedTypeRef {
                            type = original.resolvedReturnType.classId!!.toFlexibleConeClassType()
                        }
                        variance = Variance.INVARIANT
                    }
                    // replace on executor. Creates Executors.newFixedThreadPool(10)
                    val receiver = generateExecutorService()
                    explicitReceiver = receiver
                    dispatchReceiver = receiver
                    val submitSymbol = Finder.findSubmitSymbol(session).wrapJavaMethodSubmit()
                    calleeReference = buildResolvedNamedReference {
                        name = Names.SUBMIT_NAME
                        resolvedSymbol = submitSymbol
                    }
                    argumentList = buildResolvedArgumentList(
                        LinkedHashMap(mapOf(
                            buildLambdaArgumentExpression {
                                expression = buildAnonymousFunctionExpression {
                                    anonymousFunction = buildAnonymousFunction {
                                        // intentionally added to treat anonymous function as lambda
                                        source = KtRealPsiSourceElement(KtFunctionLiteral(
                                            CompositeElement(KtNodeTypes.FUNCTION_LITERAL)))
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
                                        anonFunc1.symbol.bind(anonFunc1)
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
                                                    val runBlockingSymbol = Finder.findRunBlockingSymbol(session)
                                                    calleeReference = buildResolvedNamedReference {
                                                        name = Names.RUN_BLOCKING_NAME
                                                        resolvedSymbol = runBlockingSymbol
                                                    }
                                                    argumentList = buildResolvedArgumentList(
                                                        LinkedHashMap(mapOf(
                                                            buildLambdaArgumentExpression {
                                                                expression = buildAnonymousFunctionExpression {
                                                                    anonymousFunction = buildAnonymousFunction {
                                                                        // intentionally added to treat anonymous function as lambda
                                                                        source = KtRealPsiSourceElement(KtFunctionLiteral(
                                                                            CompositeElement(KtNodeTypes.FUNCTION_LITERAL)))
                                                                        val runBlockingValParam = runBlockingSymbol.fir.valueParameters[1]
                                                                        moduleData = session.moduleData
                                                                        origin = Key.origin
                                                                        returnTypeRef = original.resolvedReturnTypeRef
                                                                        receiverTypeRef = buildResolvedTypeRef {
                                                                            type = ClassId(
                                                                                FqName.fromSegments(listOf("kotlinx", "coroutines")),
                                                                                Name.identifier("CoroutineScope")
                                                                            ).toConeClassType()
                                                                        }
                                                                        symbol = FirAnonymousFunctionSymbol()
                                                                        label = buildLabel { name = "runBlocking" }
                                                                        invocationKind = EventOccurrencesRange.EXACTLY_ONCE
                                                                        inlineStatus = InlineStatus.NoInline
                                                                        isLambda = true
                                                                        hasExplicitParameterList = false
                                                                        typeRef = buildResolvedTypeRef {
                                                                            type = ClassId(FqName.fromSegments(listOf("kotlin", "coroutines")), Name.identifier("SuspendFunction1"))
                                                                                .toConeClassType(arrayOf(
                                                                                    runBlockingValParam.returnTypeRef.coneType.typeArguments[0],
                                                                                    original.resolvedReturnType
                                                                                ), attributes = ConeAttributes.WithExtensionFunctionType)
                                                                        }
                                                                    }.also { anonFunc2 ->
                                                                        anonFunc2.symbol.bind(anonFunc2)
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
                                                                                    // todo: add real mapping
                                                                                    argumentList = buildResolvedArgumentList(LinkedHashMap())
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
                                                            } to runBlockingSymbol.fir.valueParameters[1]
                                                        ))
                                                    )
                                                }
                                            }
                                        ).apply { replaceTypeRef(original.resolvedReturnTypeRef) }
                                        )
                                    }
                                }
                            } to submitSymbol.fir.valueParameters[0]
                        ))
                    )
                }
            }
        ).apply {
                replaceTypeRef(
                    buildResolvedTypeRef {
                        type = ClassId(FqName("kotlin"), Name.identifier("Nothing")).toConeClassType()
                    }
                )
            }
    }

    @OptIn(SymbolInternals::class)
    private fun FirNamedFunctionSymbol.wrapJavaMethodSubmit(): FirNamedFunctionSymbol {
        val oldFir: FirJavaMethod = fir as FirJavaMethod
        val oldTypeParam = oldFir.typeParameters[0]
        val oldValParam = oldFir.valueParameters[0]
        bind(buildSimpleFunction {
            moduleData = oldFir.moduleData
            resolvePhase = FirResolvePhase.BODY_RESOLVE
            origin = FirDeclarationOrigin.Enhancement
            status = FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.ABSTRACT, EffectiveVisibility.Public)
            val typeParam = buildTypeParameter {
                moduleData = oldTypeParam.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Enhancement
                name = oldTypeParam.name
                symbol = FirTypeParameterSymbol()
                containingDeclarationSymbol = FirNamedFunctionSymbol(this@wrapJavaMethodSubmit.callableId).apply {
                    bind(oldFir)
                }
                variance = Variance.INVARIANT
                isReified = oldTypeParam.isReified
                bounds += buildResolvedTypeRef {
                    type = ClassId(FqName("kotlin"), Name.identifier("Any")).toFlexibleConeClassType()
                }
            }
            returnTypeRef = buildResolvedTypeRef {
                type = Names.FUTURE_CLASS_ID.toFlexibleConeClassType(arrayOf(
                    ConeFlexibleType(
                        lowerBound = ConeTypeParameterTypeImpl(lookupTag = typeParam.symbol.toLookupTag(), isNullable = false),
                        upperBound = ConeTypeParameterTypeImpl(lookupTag = typeParam.symbol.toLookupTag(), isNullable = true)
                    )
                ))
            }
            dispatchReceiverType = Names.EXECUTOR_SERVICE_CLASS_ID.toConeClassType()
            valueParameters += buildValueParameter {
                moduleData = oldValParam.moduleData
                resolvePhase = FirResolvePhase.BODY_RESOLVE
                origin = FirDeclarationOrigin.Enhancement
                returnTypeRef = buildResolvedTypeRef {
                    type = Names.CALLABLE_CLASS_ID.toFlexibleConeClassType(arrayOf(
                        ConeFlexibleType(
                            lowerBound = ConeTypeParameterTypeImpl(lookupTag = typeParam.symbol.toLookupTag(), isNullable = false),
                            upperBound = ConeTypeParameterTypeImpl(lookupTag = typeParam.symbol.toLookupTag(), isNullable = true)
                        )
                    ))
                }
                name = oldValParam.name
                symbol = FirValueParameterSymbol(oldValParam.name)
                isCrossinline = false
                isNoinline = false
                isVararg = false
            }.apply { symbol.bind(this) }
            name = Names.SUBMIT_NAME
            symbol = this@wrapJavaMethodSubmit
            oldFir.typeParameters.apply { removeLast(); add(typeParam) }
            typeParameters += typeParam
        })
        return this
    }

    @OptIn(SymbolInternals::class)
    private fun generateExecutorService(): FirFunctionCall {
        //  buildPropertyAccessExpression {
//          calleeReference = buildResolvedNamedReference {
//              name = Names.ASYNC_EXECUTOR_NAME
//              resolvedSymbol = FirNamedFunctionSymbol(Names.ASYNC_EXECUTOR_CALLABLE_ID)
//          }
//      }

        val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(
            Names.EXECUTORS_CLASS_ID
        ) as FirRegularClassSymbol
        val mySession = session
        val myScopeSession = ScopeSession()
        val sessionHolder = object : SessionHolder {
            override val session: FirSession
                get() = mySession
            override val scopeSession: ScopeSession
                get() = myScopeSession
        }
        val symbols = classSymbol.fir.staticScope(sessionHolder)?.getFunctions(Name.identifier("newFixedThreadPool"))!!

        return buildFunctionCall {
            typeRef = buildResolvedTypeRef {
                type = Names.EXECUTOR_SERVICE_CLASS_ID.toFlexibleConeClassType()
            }
            explicitReceiver = buildResolvedQualifier {
                typeRef = session.builtinTypes.unitType
                packageFqName = Names.CONCURRENT_PKG_FQN
                relativeClassFqName = FqName("Executors")
                // FirRegularClassSymbol(Names.EXECUTORS_CLASS_ID)
                symbol = Finder.findExecutorsSymbol(session)
            }
            val newThreadPoolExecutorSymbol = symbols.find { it.valueParameterSymbols.count() == 1 }!!
//                Finder.findFixedThreadPoolSymbol(session).apply {
//                val oldFir: FirJavaMethod = fir as FirJavaMethod
//                val oldValueParam = oldFir.valueParameters[0]
//                bind(buildSimpleFunction {
//                    moduleData = oldFir.moduleData
//                    resolvePhase = FirResolvePhase.BODY_RESOLVE
//                    origin = FirDeclarationOrigin.Enhancement
//                    // todo:
//                    attributes = oldFir.attributes
//                    status =
//                        FirResolvedDeclarationStatusImpl(Visibilities.Public, Modality.OPEN, EffectiveVisibility.Public)
//                    returnTypeRef = buildResolvedTypeRef {
//                        type = Names.EXECUTOR_SERVICE_CLASS_ID.toFlexibleConeClassType()
//                    }
//                    valueParameters += buildValueParameter {
//                        moduleData = oldValueParam.moduleData
//                        resolvePhase = FirResolvePhase.BODY_RESOLVE
//                        origin = FirDeclarationOrigin.Enhancement
//                        returnTypeRef = session.builtinTypes.intType
//                        name = oldValueParam.name
//                        symbol = FirValueParameterSymbol(oldValueParam.name)
//                        isCrossinline = false
//                        isNoinline = false
//                        isVararg = false
//                    }.apply { symbol.bind(this) }
//                    name = Names.NEW_FIXED_THREAD_POOL_NAME
//                    symbol = this@apply
//                })
//            }
            calleeReference = buildResolvedNamedReference {
                name = Names.NEW_FIXED_THREAD_POOL_NAME
                resolvedSymbol = newThreadPoolExecutorSymbol
            }
            argumentList = buildResolvedArgumentList(
                LinkedHashMap(
                    mapOf(
                        buildConstExpression(source = null, kind = ConstantValueKind.Int, value = 10).apply {
                            replaceTypeRef(session.builtinTypes.intType)
                        } to newThreadPoolExecutorSymbol.fir.valueParameters[0]
                    )
                )
            )
        }
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