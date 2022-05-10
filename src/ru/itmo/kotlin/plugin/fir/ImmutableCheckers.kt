package ru.itmo.kotlin.plugin.fir

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.predicate.has
import org.jetbrains.kotlin.fir.extensions.predicate.hasOrUnder
import org.jetbrains.kotlin.name.FqName
import ru.itmo.kotlin.plugin.fir.checkers.ImmutableInheritanceChecker
import ru.itmo.kotlin.plugin.fir.checkers.ImmutablePropertyChecker

/*
 * Checks that class does not contain mutable properties
 */
class ImmutableCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {

    companion object {
        val HAS_OR_UNDER_IMMUTABLE = hasOrUnder(FqName("org.itmo.my.pretty.plugin.Immutable"))
        val HAS_IMMUTABLE = has(FqName("org.itmo.my.pretty.plugin.Immutable"))
    }

    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val propertyCheckers: Set<FirPropertyChecker>
            get() = setOf(ImmutablePropertyChecker(session))
        override val classCheckers: Set<FirClassChecker>
            get() = setOf(ImmutableInheritanceChecker(session))
    }

    override fun FirDeclarationPredicateRegistrar.registerPredicates() {
        register(HAS_OR_UNDER_IMMUTABLE)
        register(HAS_IMMUTABLE)
    }
}
