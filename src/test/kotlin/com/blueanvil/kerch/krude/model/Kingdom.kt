package com.blueanvil.kerch.krude.model

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Cosmin Marginean
 */
@KrudeType(index = "middle-earth", type = "kingdom")
class Kingdom() : KrudeObject() {
    var animals: MutableList<Animal> = mutableListOf()
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = Dog::class, name = "dog"),
        JsonSubTypes.Type(value = Horse::class, name = "horse")
)
abstract class Animal(var name: String? = null)

class Dog(name: String) : Animal(name)
class Horse(name: String) : Animal(name)