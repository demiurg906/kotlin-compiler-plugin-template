package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import ru.itmo.kotlin.plugin.runners.AbstractDiagnosticTest
import ru.itmo.kotlin.plugin.runners.AbstractBoxTest

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostic/correct")
                model("diagnostic/incorrect")
                model("diagnostic/inheritance")
                model("diagnostic/innerNested")
            }
            testClass<AbstractBoxTest> {
                model("box/logging")
            }
        }
    }
}
