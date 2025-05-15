package foo.bar

import org.demiurg906.kotlin.plugin.SomeAnnotation

@SomeAnnotation
fun test() {
    val s = MyClass().foo()
    s.<!UNRESOLVED_REFERENCE!>inc<!>() // should be an error
}
