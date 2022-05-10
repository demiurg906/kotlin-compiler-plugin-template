package ru.itmo.kotlin.plugin.fir.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.warning1

object ImmutableErrors {
    val MUTABLE_PROPERTY by warning1<PsiElement, String>()
    val HAS_MUTABLE_SUPERTYPE by warning1<PsiElement, String>()
}