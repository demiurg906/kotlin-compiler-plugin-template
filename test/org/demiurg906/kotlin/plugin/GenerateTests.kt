package org.demiurg906.kotlin.plugin

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import org.demiurg906.kotlin.plugin.runners.AbstractBoxTest
import org.demiurg906.kotlin.plugin.runners.AbstractDiagnosticTest

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
            testClass<AbstractDiagnosticTest> {
                model("diagnostics")
            }

            testClass<AbstractBoxTest> {
                model("box")
            }
        }
    }
}
