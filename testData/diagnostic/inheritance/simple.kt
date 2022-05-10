package foo.bar

import org.itmo.my.pretty.plugin.Immutable

@Immutable
open class ImmutableBase

open class MutableBase

@Immutable
class ImmutableImmutable : ImmutableBase()

<!HAS_MUTABLE_SUPERTYPE!>@Immutable
class MutableImmutable : MutableBase()<!>

class ImmutableMutable : ImmutableBase()

class MutableMutable : MutableBase()
