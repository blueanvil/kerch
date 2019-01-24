package com.blueanvil.kerch.krude

import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.util.*

data class Bottom(val name: String,
                  var roles: MutableSet<String> = HashSet()) : Top() {

    var something: String? = null
    var properties: Properties = Properties()

    @JsonSerialize(using = CustomIntSerializer::class)
    @JsonDeserialize(using = CustomIntDeserializer::class)
    var someInt: Int = 20

    var dog: Dog? = null

    var somethingElse: MutableSet<String> = HashSet()
}

data class Dog(val name: String)