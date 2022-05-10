package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import ru.itmo.kotlin.plugin.fir.ImmutableCheckers

fun getAllImmutableClasses(session: FirSession): Set<ClassId> {
    return StandardClassIds.constantAllowedTypes + StandardClassIds.Any + session.predicateBasedProvider
        .getSymbolsByPredicate(ImmutableCheckers.HAS_IMMUTABLE)
        .filterIsInstance<FirRegularClassSymbol>()
        .map { it.classId }
}

fun hasOrUnderImmutable(session: FirSession, declaration: FirDeclaration): Boolean {
    return session.predicateBasedProvider.matches(ImmutableCheckers.HAS_OR_UNDER_IMMUTABLE, declaration)
}

fun hasImmutable(session: FirSession, declaration: FirDeclaration): Boolean {
    return session.predicateBasedProvider.matches(ImmutableCheckers.HAS_IMMUTABLE, declaration)
}
