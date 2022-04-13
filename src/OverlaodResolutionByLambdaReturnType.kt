fun test(l: List<String>, sequence: Sequence<Int>) {
    l.flatMap { it.split(" ") } // List<String>
    l.flatMap { it.split(" ").asSequence() } // Sequence<String>

    /*
     * some candidates >= 2
     * all acceptable
     * one lambda argument
     * lambda in all candidates have same input types
     * some of candidates have @OverloadResolutionByLambdaReturnType annotations
     *
     *
     * (T) > Iterable<R>
     * (T) -> _R
     *
     * _R <: Iterable<R>
     * _R <: Sequence<R>
     *
     * List<String> <: _R
     */

}

// fun <T, R> Iterable<T>.flatMap(transform: (T) -> Iterable<R>): List<R>
// fun <T, R> Iterable<T>.flatMap(transform: (T) -> Sequence<R>): Sequence<R>