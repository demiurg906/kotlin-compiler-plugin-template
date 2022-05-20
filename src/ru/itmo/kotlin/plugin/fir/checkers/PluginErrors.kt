package ru.itmo.kotlin.plugin.fir.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error2

object PluginErrors {
    val ILLEGAL_ANNOTATED_FUNCTION by error2<PsiElement, String, String>(SourceElementPositioningStrategies.ANNOTATION_USE_SITE)
}