package foo.bar

import org.itmo.my.pretty.plugin.*
import org.itmo.my.pretty.plugin.Injectable
import org.itmo.my.pretty.plugin.Injected

@Injected(name = "x"<!MULTIPLE_NAME_DEFINITIONS!>)<!>
class InjectedImpl

@Injected(name = "x"<!MULTIPLE_NAME_DEFINITIONS!>)<!>
class InjectedImpl2