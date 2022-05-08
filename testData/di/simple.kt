package foo.bar

import org.itmo.my.pretty.plugin.*
import org.itmo.my.pretty.plugin.Injectable
import org.itmo.my.pretty.plugin.Injected

//1. Всякое Dependency Injection (можно, например, автоматом добавлять поля для всех injectable сущностей)
@Injected(name = "x")
class InjectedImpl {
    val inner = 3
}

@Injectable
class InjectableImpl {}

fun box(): String {
    val impl = InjectableImpl()
    return if (impl.x.inner == 3) {
        "OK"
    } else {
        "Fail: $impl"
    }
}