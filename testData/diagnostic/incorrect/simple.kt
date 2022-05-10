package foo.bar

import org.itmo.my.pretty.plugin.Immutable

@Immutable
class ImmutableBase

@Immutable
class MyImmutable {
    <!MUTABLE_PROPERTY!>var a: Int = 42<!>
    <!MUTABLE_PROPERTY!>val b: MyMutable = MyMutable()<!>
    <!MUTABLE_PROPERTY!>var c: ImmutableBase = ImmutableBase()<!>
}

class MyMutable {
    <!MUTABLE_PROPERTY!>@Immutable
    var a: Int = 42<!>
}
