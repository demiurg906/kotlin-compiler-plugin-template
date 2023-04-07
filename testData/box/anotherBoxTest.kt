// WITH_STDLIB

fun box(): String {
    val list = listOf("aaa", "bb", "c")
    val result = list.map { it.length }.sum()
    return if (result == 6) "OK" else "Fail: $result"
}
