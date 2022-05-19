package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.fir.DependencyInjector

class InjectedConstructorChecker(private val session: FirSession) : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!declaration.hasAnnotation(ClassId(DependencyInjector.PACKAGE, Name.identifier("Injected")))) return
        val constructor = declaration.primaryConstructorIfAny(session)
        if (constructor == null || constructor.valueParameterSymbols.isNotEmpty() || constructor.typeParameterSymbols.isNotEmpty()) {
            reporter.reportOn(declaration.source, PluginErrors.NO_CONSTRUCTOR, "Injected service should have primary constructor without parameters", context)
        }
    }
}