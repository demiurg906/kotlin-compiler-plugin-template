package foo.bar

import org.itmo.my.pretty.plugin.*
import org.itmo.my.pretty.plugin.Injectable
import org.itmo.my.pretty.plugin.Injected

// To check that if the annotation name is not `Injected`, duplicate `name` arguments are not marked as errors
annotation class OtherAnnotation(val name: String)
@OtherAnnotation(name = "x")
class ClassWithoutErrors

//1. Всякое Dependency Injection (можно, например, автоматом добавлять поля для всех injectable сущностей)
var ext = 0
@Injected(name = "x")
class InjectedImpl {
    init {
        ext += 1
    }
    val inner = 3
}

@Injectable
class InjectableImpl {}

@Injectable
class InjectableImpl2 {}

fun box(): String {
    val impl = InjectableImpl()
    if (impl.x.inner != 3 || impl.x.inner != 3) return "Fail with wrong initializer: $impl"
    if (ext != 1) return "Initialized $ext times instead: $impl"
    val impl2 = InjectableImpl2()
    return if (impl2.x.inner == 3 && ext == 1) {
        "OK"
    } else {
        "Fail: initiialized $ext times $impl2"
    }
}