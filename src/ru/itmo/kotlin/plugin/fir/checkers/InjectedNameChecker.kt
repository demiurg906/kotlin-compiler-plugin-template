/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package ru.itmo.kotlin.plugin.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirAnnotationCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirCallChecker
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirCall
import org.jetbrains.kotlin.fir.expressions.FirConstExpression
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.resolve.dfa.symbol
import org.jetbrains.kotlin.fir.resolve.fqName
import org.jetbrains.kotlin.name.Name
import ru.itmo.kotlin.plugin.fir.DependencyInjector

class InjectedNameChecker(val session: FirSession) : FirAnnotationCallChecker() {
    private val names = mutableMapOf<String, FirCall>()
    override fun check(expression: FirAnnotationCall, context: CheckerContext, reporter: DiagnosticReporter) {
        if (expression.fqName(session) != DependencyInjector.injectedFQ) return
        println(">>>>>")
        val arg = expression.findArgumentByName(Name.identifier("name"))
        val argValue = (arg as? FirConstExpression<*>)?.value as? String
        if (argValue == null) {
            reporter.reportOn(expression.source, PluginErrors.WRONG_NAME_FORMAT, "Wrong name for injectable annotation $expression", "", context)
            return
        }
        val foundAnnotation = names[argValue]
        if (foundAnnotation != null) {
            reporter.reportOn(expression.source, PluginErrors.MULTIPLE_NAME_DEFINITIONS, "Injected service with the same name already exists", argValue, context)
            reporter.reportOn(foundAnnotation.source, PluginErrors.MULTIPLE_NAME_DEFINITIONS, "Injected service with the same name already exists", argValue, context)
        } else {
            names[argValue] = expression
        }
    }

}

object DummyNameChecker : FirSimpleFunctionChecker() {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val name = declaration.name.asString()
        if (name == "dummy") {
            reporter.reportOn(declaration.source, PluginErrors.FUNCTION_WITH_DUMMY_NAME, name, context)
        }
    }
}
