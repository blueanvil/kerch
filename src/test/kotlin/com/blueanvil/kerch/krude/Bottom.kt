package com.blueanvil.kerch.krude

import java.util.*

data class Bottom(val name: String,
                  var roles: MutableSet<String> = HashSet()) : Top() {

    var something: String? = null
    var properties: Properties = Properties()
    var someInt: Int = 20
    var somethingElse: MutableSet<String> = HashSet()
}