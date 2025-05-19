// RUN_PIPELINE_TILL: FRONTEND

// MODULE: lib
// FILE: foo.kt
package foo

fun takeInt(x: Int) {}

fun test() {
    takeInt(10)
}

// MODULE: main(lib)
// FILE: bar.kt
package bar

import foo.takeInt

fun test() {
    takeInt(10)
    takeInt(<!ARGUMENT_TYPE_MISMATCH!>"Hello"<!>)
}
