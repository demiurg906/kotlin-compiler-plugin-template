package ru.itmo.kotlin.plugin

import org.jetbrains.kotlin.generators.generateTestGroupSuiteWithJUnit5
import ru.itmo.kotlin.plugin.runners.AbstractDITest
import ru.itmo.kotlin.plugin.runners.AbstractDiagnosticTest
import ru.itmo.kotlin.plugin.runners.AbstractNdArrayTest

fun main() {
    generateTestGroupSuiteWithJUnit5 {
        testGroup(testDataRoot = "testData", testsRoot = "test-gen") {
            testClass<AbstractDiagnosticTest> {
                model("di_err")
            }

            testClass<AbstractDITest> {
                model("di")
            }

            testClass<AbstractNdArrayTest> {
                model("ndArray")
            }
        }
    }
}
