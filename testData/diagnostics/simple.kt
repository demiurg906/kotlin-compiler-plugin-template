// FULL_JDK

package foo.bar

import ru.itmo.kotlin.plugin.desuspender.DeSuspend


<!WRONG_ANNOTATION_TARGET!>@DeSuspend<!>
class MyClass {
    suspend fun foo() = "Hello, world!"
}

<!ILLEGAL_ANNOTATED_FUNCTION!>@DeSuspend
fun test() {
    val s = MyClass()
    s.<!UNRESOLVED_REFERENCE!>fooAsync<!>()
}<!>
