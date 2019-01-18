package com.blueanvil.krude.json

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.Krudes
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import kotlin.reflect.full.memberProperties

/**
 * @author Cosmin Marginean
 */
class KrudeObjectSerializer : JsonSerializer<KrudeObject>() {

    override fun serialize(value: KrudeObject, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeStartObject()

        gen.writeFieldName(Krudes.annotation(value::class).type)

        gen.writeStartObject()
        value.javaClass.kotlin.memberProperties.forEach {
            gen.writeFieldName(it.name)
            gen.writeObject(it.get(value))
        }
        gen.writeEndObject()

        gen.writeEndObject()
    }
}
