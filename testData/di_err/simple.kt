package foo.bar

import org.itmo.my.pretty.plugin.*
import org.itmo.my.pretty.plugin.Injectable
import org.itmo.my.pretty.plugin.Injected

@Injected(name = "x"<!MULTIPLE_NAME_DEFINITIONS!>)<!>
class InjectedImpl

@Injected(name = "x"<!MULTIPLE_NAME_DEFINITIONS!>)<!>
class InjectedImpl2

// To check that each diagnostic applies only once
@Injected(name = "x"<!MULTIPLE_NAME_DEFINITIONS!>)<!>
class InjectedImpl3

@Injected(name = "y")
class <!NO_CONSTRUCTOR!>InjectedImpl4<!>(val data: Int) {}