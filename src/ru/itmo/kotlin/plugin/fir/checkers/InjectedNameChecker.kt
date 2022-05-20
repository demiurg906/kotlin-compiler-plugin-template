package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.fir.DependencyInjector
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

object InjectedNameChecker : FirAnnotationCallChecker() {
    private val names = ConcurrentHashMap<String, AtomicReference<FirCall?>>()

    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.fqName(context.session) != DependencyInjector.injectedFQ) return
        val arg = expression.findArgumentByName(Name.identifier("name"))
        val argValue = (arg as? FirConstExpression<*>)?.value as? String
        if (argValue == null) {
            reporter.reportOn(expression.source, PluginErrors.WRONG_NAME_FORMAT, "Wrong name for injectable annotation $expression", context)
            return
        }
        names.compute(argValue) { _, foundAnnotation ->
            if (foundAnnotation != null) {
                var value = foundAnnotation.get()
                while (!foundAnnotation.compareAndSet(value, null)) {
                    value = foundAnnotation.get()
                }
                listOf(value?.source, expression.source).forEach { src ->
                    reporter.reportOn(src, PluginErrors.MULTIPLE_NAME_DEFINITIONS, "Injected service with the same name already exists", context)
                }
                AtomicReference(null)
            } else AtomicReference(expression)
        }
    }
}