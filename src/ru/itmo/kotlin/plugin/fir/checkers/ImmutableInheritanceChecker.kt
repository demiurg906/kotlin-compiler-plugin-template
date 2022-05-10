package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.fir.types.coneType

class ImmutableInheritanceChecker(private val session:  FirSession) : FirClassChecker() {
    private val allImmutableClasses by lazy { getAllImmutableClasses(session) }

    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!hasImmutable(session, declaration)) return

        if (!declaration.superTypeRefs.all { it.coneType.classId in allImmutableClasses }) {
            reporter.reportOn(declaration.source, ImmutableErrors.HAS_MUTABLE_SUPERTYPE, declaration.toString(), context)
        }
    }
}
