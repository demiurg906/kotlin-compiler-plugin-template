// FULL_JDK
package foo.bar

import ru.itmo.kotlin.plugin.desuspender.*
import ru.itmo.kotlin.plugin.desuspender.DeSuspend

class SimpleClass() {
    @DeSuspend
    suspend fun topLevelSuspend(x: Double = 0.42): String {
        return x.toString()
    }
}

fun box(): String {
    SimpleClass().topLevelSuspendAsync(0.5)
    return "OK"
}
