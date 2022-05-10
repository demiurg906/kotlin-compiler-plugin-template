package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import ru.itmo.kotlin.plugin.runners.AbstractDiagnosticTest

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData/diagnostic", testsRoot = "test-gen") {
            testClass<AbstractDiagnosticTest> {
                model("correct")
                model("incorrect")
                model("inheritance")
                model("innerNested")
            }
        }
    }
}
