package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.nestie.NestieDoc
import com.blueanvil.kerch.uuid
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.testng.Assert.assertEquals

/**
 * @author Cosmin Marginean
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(
        JsonSubTypes.Type(value = Magazine::class, name = "magazine"),
        JsonSubTypes.Type(value = Tabloid::class, name = "tabloid")
)
@NestieDoc(type = "publication")
abstract class Publication(val name: String,
                           val publisher: String,
                           var id: String = uuid(),
                           var version: Long = 0)

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
    assertEquals(p1::class, p2::class)
    assertEquals(p1.id, p2.id)
    assertEquals(p1.name, p2.name)
    assertEquals(p1.publisher, p2.publisher)
    if (p1 is Magazine && p2 is Magazine) {
        assertEquals(p1.monthly, p2.monthly)
    } else if (p1 is Tabloid && p2 is Tabloid) {
        assertEquals(p1.audience, p2.audience)
    }
}