class A {
    companion object {
        @JvmStatic
        fun bar() {}
    }
}

fun A.Companion.foo() {}

fun test() {
    A.foo()
}