// FULL_JDK
package foo.bar

import ru.itmo.kotlin.plugin.desuspender.*
import ru.itmo.kotlin.plugin.desuspender.DeSuspend

import kotlinx.coroutines.runBlocking

@DeSuspend
suspend fun checkThatGreaterThan42(x: Int): Boolean {
    return x > 42
}

@DeSuspend
suspend fun execFoo(obj: SimpleClass) {
    obj.foo(y = "")
}

class SimpleClass() {
    @DeSuspend
    suspend fun foo(x: Double = 0.42, unused: Int = 42, y: String): String {
        return "$y is $x"
    }

    companion object {
        @DeSuspend
        suspend fun fooStatic(message: String = "world") = "Hello, $message!"
    }
}

object SimpleObject {
    @DeSuspend
    suspend fun bar(start: Int = 0, step: Int = 1, count: Int = 10): Int {
        var result = 0
        for (i in start until (count * step + start) step(step)) {
            result += i
        }
        return result
    }
}

fun box(): String {
    val simpleClass = SimpleClass()
    val results = runBlocking {
        return@runBlocking listOf(
            checkThatGreaterThan42(43) == checkThatGreaterThan42Async(43).get(),
            simpleClass.foo(x = 0.5, y = "good") == simpleClass.fooAsync(y = "good", x = 0.5).get(),
            SimpleClass.fooStatic("world") == SimpleClass.fooStaticAsync().get(),
            SimpleObject.bar(10, 5, 10) == SimpleObject.barAsync(10, 5, 10).get()
        )
    }
    return if (results.all { it }) "OK" else "error: some values are not equals"
}
