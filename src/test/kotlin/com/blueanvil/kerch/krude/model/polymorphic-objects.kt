package com.blueanvil.kerch.krude.model

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.junit.Assert

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

fun assertSamePublication(p1: Publication, p2: Publication) {
    Assert.assertEquals(p1::class, p2::class)
    Assert.assertEquals(p1.id, p2.id)
    Assert.assertEquals(p1.name, p2.name)
    Assert.assertEquals(p1.publisher, p2.publisher)
    if (p1 is Magazine && p2 is Magazine) {
        Assert.assertEquals(p1.monthly, p2.monthly)
    } else if (p1 is Tabloid && p2 is Tabloid) {
        Assert.assertEquals(p1.audience, p2.audience)
    }
}