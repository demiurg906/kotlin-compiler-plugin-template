package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirPropertyChecker
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.hasBackingField
import org.jetbrains.kotlin.fir.types.classId

class ImmutablePropertyChecker(private val session:  FirSession) : FirPropertyChecker() {
    private val allImmutableClasses by lazy { getAllImmutableClasses(session) }

    override fun check(declaration: FirProperty, context: CheckerContext, reporter: DiagnosticReporter) {
        val classId = declaration.symbol.resolvedReturnTypeRef.type.classId

        if (!hasOrUnderImmutable(session, declaration)) return
        if (classId in allImmutableClasses && declaration.setter == null) return
        if (!declaration.hasBackingField) return

        reporter.reportOn(declaration.source, ImmutableErrors.MUTABLE_PROPERTY, declaration.name.asString(), context)
    }
}
