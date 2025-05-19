# Kotlin Compiler Plugin template

This is a template project for writing a compiler plugin for the Kotlin compiler.

## Details

This project has three modules:
- The [`:compiler-plugin`](compiler-plugin/src) module contains the compiler plugin itself.
- The [`:plugin-annotations`](plugin-annotations/src/commonMain/kotlin) module contains annotations which can be used in
user code for interacting with compiler plugin.
- The [`:gradle-plugin`](gradle-plugin/src) module contains a simple Gradle plugin to add the compiler plugin and
annotation dependency to a Kotlin project. 

Extension point registration:
- K2 Frontend (FIR) extensions can be registered in `SimplePluginRegistrar`.
- All other extensions (including K1 frontend and backend) can be registered in `SimplePluginComponentRegistrar`.

## Tests

The [Kotlin compiler test framework][test-framework] is set up for this project.
To create a new test, add a new `.kt` file in a [compiler-plugin/testData](compiler-plugin/testData) sub-directory:
`testData/box` for codegen tests and `testData/diagnostics` for diagnostics tests.
The generated JUnit 5 test classes will be updated automatically when tests are next run.
They can be manually updated with the `generateTests` Gradle task as well.
To aid in running tests, it is recommended to install the [Kotlin Test Data Helper][test-plugin] IntelliJ plugin,
which is pre-configured in this repository.

[//]: # (Links)

[test-framework]: https://github.com/JetBrains/kotlin/blob/2.1.20/compiler/test-infrastructure/ReadMe.md
[test-plugin]: https://github.com/demiurg906/test-data-helper-plugin
