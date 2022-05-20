package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.primaryConstructorIfAny
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import ru.itmo.kotlin.plugin.fir.DependencyInjector

object InjectedConstructorChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!context.session.predicateBasedProvider.matches(DependencyInjector.INJECTED_PREDICATE, declaration))
            return
        val constructor = declaration.primaryConstructorIfAny(context.session)
        if (constructor == null || constructor.valueParameterSymbols.isNotEmpty() || constructor.typeParameterSymbols.isNotEmpty()) {
            reporter.reportOn(declaration.source, PluginErrors.NO_CONSTRUCTOR, "Injected service should have primary constructor without parameters", context)
        }
    }
}