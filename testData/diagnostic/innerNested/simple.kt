package foo.bar

import org.itmo.my.pretty.plugin.Immutable

@Immutable
class ImmutableOuter {
    @Immutable
    class ImmutableNested
    class MutableNested

    @Immutable
    inner class ImmutableInner
    inner class MutableInner
}

class MutableOuter {
    @Immutable
    class ImmutableNested
    class MutableNested

    @Immutable
    inner class ImmutableInner
    inner class MutableInner
}

@Immutable
class MyImmutable {
    val a = ImmutableOuter.ImmutableNested()
    <!MUTABLE_PROPERTY!>val b = ImmutableOuter.MutableNested()<!>
    val c = ImmutableOuter().ImmutableInner()
    <!MUTABLE_PROPERTY!>val d = ImmutableOuter().MutableInner()<!>
    val e = MutableOuter.ImmutableNested()
    <!MUTABLE_PROPERTY!>val f = MutableOuter.MutableNested()<!>
    val g = MutableOuter().ImmutableInner()
    <!MUTABLE_PROPERTY!>val h = MutableOuter().MutableInner()<!>
}