package ru.itmo.kotlin.plugin.fir

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClassForStaticMemberAttr
import org.jetbrains.kotlin.fir.declarations.FirPluginKey
import org.jetbrains.kotlin.fir.declarations.builder.buildBackingField
import org.jetbrains.kotlin.fir.declarations.builder.buildProperty
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.extensions.FirDeclarationGenerationExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.DeclarationPredicate
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.ConeClassLikeType
import org.jetbrains.kotlin.fir.types.ConeTypeProjection
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.realElement

class DependencyInjector(session: FirSession) : FirDeclarationGenerationExtension(session) {
    companion object {
        private val PACKAGE = FqName.topLevel(Name.identifier("org.itmo.my.pretty.plugin"))
        val injectedFQ = FqName("org.itmo.my.pretty.plugin.Injected")
        val INJECTED_PREDICATE: DeclarationPredicate = has(injectedFQ)
        val injectableFQ = FqName("org.itmo.my.pretty.plugin.Injectable")
        val INJECTABLE_PREDICATE: DeclarationPredicate = has(injectableFQ)
    }

    private val predicateBasedProvider = session.predicateBasedProvider
    private val matchedInjected by lazy { predicateBasedProvider.getSymbolsByPredicate(INJECTED_PREDICATE).filterIsInstance<FirRegularClassSymbol>() }
    private val matchedInjectable by lazy { predicateBasedProvider.getSymbolsByPredicate(INJECTABLE_PREDICATE).filterIsInstance<FirRegularClassSymbol>() }

    private val FirBasedSymbol<*>.nameArg: String
        get() {
            val annotation = annotations.find { it.fqName(session) == injectedFQ } ?: throw IllegalArgumentException("No annotation with name $injectedFQ")
            val arg = annotation.findArgumentByName(Name.identifier("name"))
            return ((arg as? FirConstExpression<*>)?.value as? String) ?: throw IllegalArgumentException("No name attribute in annotation $injectedFQ")
        }

    override fun generateProperties(callableId: CallableId, owner: FirClassSymbol<*>?): List<FirPropertySymbol> {
        if (owner == null || !owner.annotations.any { it.fqName(session) == injectableFQ }) return emptyList()
        return matchedInjected.map {
            val psymbol = FirPropertySymbol(callableId)
            val returnType = buildResolvedTypeRef { type = it.classId.toConeType() }
            val bName = Name.identifier("$${callableId.callableName.asString()}")
            val pStatus = FirResolvedDeclarationStatusImpl(
                Visibilities.Public, Modality.FINAL, EffectiveVisibility.Public
            )
            buildProperty {
                getter = FirDefaultPropertyGetter(
                    source = owner.source,
                    moduleData = session.moduleData,
                    origin = Key.origin,
                    propertyTypeRef = returnType,
                    visibility = Visibilities.Public,
                    propertySymbol = psymbol,
                    effectiveVisibility = EffectiveVisibility.Public
                )

                moduleData = session.moduleData
                origin = Key.origin
                status = pStatus
                isVar = false
                isLocal = false
                symbol = psymbol
                name = callableId.callableName
                returnTypeRef = returnType
            }.also { it.containingClassForStaticMemberAttr = ConeClassLikeLookupTagImpl(owner.classId) }.symbol
        }
    }

    private fun ClassId.toConeType(typeArguments: Array<ConeTypeProjection> = emptyArray()): ConeClassLikeType {
        val lookupTag = ConeClassLikeLookupTagImpl(this)
        return ConeClassLikeTypeImpl(lookupTag, typeArguments, isNullable = false)
    }

    override fun getCallableNamesForClass(classSymbol: FirClassSymbol<*>): Set<Name> {
        return if (matchedInjectable.contains(classSymbol)) {
            matchedInjected.map { Name.identifier(it.nameArg) }.toSet()
        } else {
            emptySet()
        }
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(INJECTED_PREDICATE)
        register(INJECTABLE_PREDICATE)
    }

    object Key : FirPluginKey() {
        override fun toString(): String {
            return "DependencyInjector"
        }
    }
}
