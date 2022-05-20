package ru.itmo.kotlin.plugin.fir.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.error0

object PluginErrors {
    val ILLEGAL_ANNOTATED_FUNCTION by error0<PsiElement>()
}