package foo.bar

fun box(): String {
    val result = MyClass().foo()
    return if (result == "Hello wovrld") { "OK" } else { "Fail: $result" }
}
