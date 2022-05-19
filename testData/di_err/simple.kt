package foo.bar

import org.itmo.my.pretty.plugin.*
import org.itmo.my.pretty.plugin.Injectable
import org.itmo.my.pretty.plugin.Injected

@Injected(name = "x"<!MULTIPLE_NAME_DEFINITIONS!>)<!>
class InjectedImpl

@Injected(name = "x"<!MULTIPLE_NAME_DEFINITIONS!>)<!>
class InjectedImpl2

@Injected(name = "y")
class [<!NO_CONSTRUCTOR!>InjectedImpl3<!>(val data: Int) {}