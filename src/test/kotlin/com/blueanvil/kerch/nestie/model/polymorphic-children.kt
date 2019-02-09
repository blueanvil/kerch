package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.nestie.NestieDoc
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Cosmin Marginean
 */
@NestieDoc(index = "world", type = "kingdom")
class Kingdom(val animals: List<Animal>) : ElasticsearchDocument()

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = Dog::class, name = "dog"),
        JsonSubTypes.Type(value = Horse::class, name = "horse")
)
abstract class Animal(val name: String)

class Dog(name: String,
          val breed: String) : Animal(name)

class Horse(name: String,
            val age: Int) : Animal(name)



