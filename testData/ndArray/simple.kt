package foo.bar
fun box(): String {
    val x = Array2(5) { arrayOf(1, 2, 3) }
    return if (x[0][0] != 1 || x[0][1] != 2 || x[0][2] != 3) "Wrong array generated: $x" else "OK"
}