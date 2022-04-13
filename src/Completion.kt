object Completion {
    fun <T> unwrap(list: List<T>): T = list.first()

    fun test(x: List<Int>) {
        val y = unwrap(x)
    }

/*
 * T <: Any? // from bound
 * List<Int> <: List<T> // from argument
 * Int <: T // from incorporation
 */

    fun <K> materialize(): K = null!!
    fun <T> id(x: T): T = x

    fun takeInt(x: Number) {}
    fun takeInt(x: Int) {}

    fun test() {
        val x: Int = materialize()
        val y: Int = id(materialize())

        takeInt(materialize())

        takeInt(id(materialize()))
    }

/*
 * takeInt(materialize())
 *   for materialize()
 *   K <: Any?
 *
 * for takeInt
 *   K <: Any?
 *   K <: Int
 *
 *   K == Int
 */

/*
 * takeInt(id(materialize()))
 *   for materialize()
 *    K <: Any?
 *
 *   for id(materialize())
 *    K <: T
 *
 *   for takeInt(id(materialize()))
 *    T <: Int
 *
 *   result system:
 *    K <: Any?
 *    K <: T
 *    T <: Int
 *
 *  1. T == Int
 *    K <: Any?
 *    K <: T
 *    T == Int // fixation
 *    K <: Int // new
 *  2. K == Int
 *
 */
}