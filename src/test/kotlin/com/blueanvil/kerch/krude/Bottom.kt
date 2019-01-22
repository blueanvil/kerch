package com.blueanvil.kerch.krude

data class Bottom(val name: String,
                  var roles: MutableSet<String> = HashSet()) : Top() {

    var something: String? = null
    var someInt: Int = 20
    var somethingElse: MutableSet<String> = HashSet()
}