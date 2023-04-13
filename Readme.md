# Kotlin Compiler Plugin template

This is a template project for compiler plugins for K2 Kotlin compiler

## Details

Project contains two modules:
- root module is a module for the compiler plugin itself
- `:plugin-annotations` module contains annotations which can be used in user code for interacting with compiler plugin

Extension point registration:
- K2 Frontend (FIR) extensions can be registered in `SimplePluginRegistrar`
- All other extensions (including K1 frontend and backend) can be registered in `SimplePluginComponentRegistrar`

## Tests

Kotlin compiler test framework is set up for this project. To add a new test you need to put new `.kt` file in `testData` 
directory (`testData/box` for codegen tests and `testData/diagnostics` for diagnostics tests) and run `:generateTests`
Gradle task. This task will update generated tests classes and generate new tests methods for added tests
