object Lambdas {

    fun <T> foo(x: Int, b: T, f: T.() -> T) {}
    fun <T> foo(x: String, b: T, f: (T) -> T) {}

    fun test() {
        // () -> T          1
        // (U) -> T         2
        // R.() -> T        3
        // R.(U) -> T       4
        foo("", 10) {
            println()
            it + 1
        }

        // String.() -> String
        foo(10, "") {
            this + "bar"
        }

        // Function0<Int> <: Function0<T>
    }

    fun <T> lateMaterialize(block: (T) -> Unit): T {
        TODO()
    }

    fun <K> id(x: K): K = x

    fun takeInt(x: Int) {}

    fun test_2() {
        // FirAnonymousFunction
        // valueParameters = []
        // body = FirBlock@xxx

        // FirAnonymousFunction
        // valueParameters = [it: Int]
        // body = FirBlock@xxx
        takeInt(id(lateMaterialize { println(it + 1) }))
    }
}