package com.blueanvil.kerch.krude

data class Bottom(val name: String,
                  var roles: MutableSet<String> = HashSet()) : Top() {

    var something: String? = null
    var somethingElse: MutableSet<String> = HashSet()
}