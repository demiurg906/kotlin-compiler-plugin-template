package foo.bar

class Persistence
class X

context(Persistence, X)
class Repo(val n: Int)

fun f(): Int {
    println("123")
    return with<X, Int>(X()) {
        with<Persistence, Int>(Persistence()) {
            Repo(0).n
        }
    }
}

fun box(): String {
    val result = f()
    return if (result == 0) {
        "OK"
    } else {
        "Fail: $result"
    }
}
