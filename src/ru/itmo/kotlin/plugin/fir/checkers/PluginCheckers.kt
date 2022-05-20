package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.name.ClassId
import ru.itmo.kotlin.plugin.fir.Names

object AnnotatedIsSuspendFunctionChecker : FirSimpleFunctionChecker() {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration.hasAnnotation(
                ClassId(
                    Names.PACKAGE_FQN,
                    Names.ANNOTATION_NAME
                )
            ) && !declaration.status.isSuspend
        ) {
            reporter.reportOn(declaration.source, PluginErrors.ILLEGAL_ANNOTATED_FUNCTION, context)
        }
    }
}
