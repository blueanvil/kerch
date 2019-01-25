package com.blueanvil.kerch.krude.model

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

/**
 * @author Cosmin Marginean
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = Magazine::class, name = "magazine"),
        JsonSubTypes.Type(value = Tabloid::class, name = "tabloid")
)
@KrudeType(index = "content", type = "publication")
abstract class Publication(val name: String,
                           val publisher: String) : KrudeObject()

class Magazine(name: String,
               publisher: String) : Publication(name, publisher) {
    var monthly: Boolean = false
}

class Tabloid(name: String,
              publisher: String,
              val audience: AudienceType) : Publication(name, publisher) {
}

enum class AudienceType {
    ADULT,
    TEENAGE
}

