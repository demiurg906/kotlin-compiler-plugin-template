package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import ru.itmo.kotlin.plugin.runners.AbstractDiTest
import ru.itmo.kotlin.plugin.runners.AbstractBoxTest
import ru.itmo.kotlin.plugin.runners.AbstractDiagnosticTest

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractBoxTest> {
                model("box")
            }

            testClass<AbstractDiTest> {
                model("di")
            }
        }
    }
}
