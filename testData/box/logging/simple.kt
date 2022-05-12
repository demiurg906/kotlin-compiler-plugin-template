package foo.bar

import org.itmo.my.pretty.plugin.LogActualClass

open class Base {
    @LogActualClass
    open fun foo() {
        println("foo")
    }

    open fun bar() { }

    open fun baz() { }
}

class Derived : Base() {
    override fun foo() { }

    @LogActualClass
    override fun bar() { }

    override fun baz() { }
}


fun callFuns(x: Base) {
    x.foo()
    x.bar()
    x.baz()
}

fun box(): String {
    callFuns(Base())
    callFuns(Derived())
    return "OK"
}
