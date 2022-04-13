object Subtyping {
    fun <T : Any> id(x: T): T = x // (1)

    fun test() {
        val x: CharSequence = id("string")
    }

/*
 * T <: Any // from typ parameter bound
 * String <: T // from argument
 * T <: CharSequence // from expected type
 *
 *
 * argumentType <: parameterType
 */

    fun foo(x: Int) {}

    fun test_2() {
//        foo("string")

        // String <: Int
        // has contradiction
    }

    fun takeCollection(c: Collection<CharSequence>) {}
    fun testList(list: List<String>) {
        takeCollection(list)
    }

/*
 * isSubtypeOf(List<String>, Collection<CharSequence>)
 * 1. List<String> has supertype Collection<String>
 * 2. is Collection<String> subtype of Collection<CharSequence> ?
 * 3. is String <: CharSequence (because E is out E in Collection)
 */

    interface InvBase<T>
    interface InvDerived<T> : InvBase<T>

/*
 * isSubtypeOf(InvDerived<String>, InvBase<CharSequence>)
 * 1. InvDerived<String> has supertype InvBase<String>
 * 2. is InvBase<String> subtype of InvBase<CharSequence> ?
 * 3. is String == CharSequence (because E is inv E in InvBase)
 */


    class Inv<T>
    class Out<out T>
    class In<in T>

/*
 * Inv<T> <: Inv<R>   <===>   T == R
 * Out<T> <: Out<R>   <===>   T <: R
 * In<T> <: In<R>     <===>   T :> R
 */

}