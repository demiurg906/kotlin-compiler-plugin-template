package foo.bar

import org.itmo.my.pretty.plugin.Immutable

@Immutable
class ImmutableBase

@Immutable
class MyImmutable {
    val a: Int = 1
    val b: ImmutableBase = ImmutableBase()
    val c: Int
        get(): Int = 32
    var d: Int
        get(): Int = 32
        set(value) { }
}

class MyMutable {
    val a: Int = 1
    val b: ImmutableBase = ImmutableBase()
    val c: Int
        get(): Int = 32
    var d: Int = 1
    var e: ImmutableBase = ImmutableBase()
    var f: Int
        get(): Int = 32
        set(value) { }
}