/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package ru.itmo.kotlin.plugin.fir.checkers

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1

object PluginErrors {
    val NO_CONSTRUCTOR by error1<PsiElement, String>(SourceElementPositioningStrategies.NAME_IDENTIFIER)
    val MULTIPLE_NAME_DEFINITIONS by error1<PsiElement, String>(SourceElementPositioningStrategies.VALUE_ARGUMENTS)
    val WRONG_NAME_FORMAT by error1<PsiElement, String>(SourceElementPositioningStrategies.VALUE_ARGUMENTS)
}
