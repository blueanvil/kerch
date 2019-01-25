package com.blueanvil.kerch.krude.json

import com.blueanvil.kerch.krude.KrudeObjectWrapper
import com.blueanvil.kerch.krude.Krudes
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * @author Cosmin Marginean
 */
class KrudeSerializer(private val objectMapper: ObjectMapper,
                      private val krudes: Krudes) : JsonSerializer<KrudeObjectWrapper>() {

    override fun serialize(value: KrudeObjectWrapper, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeStartObject()
        gen.writeFieldName(krudes.classesToTypes[value.krudeObject::class.java])
        objectMapper.writeValue(gen, value.krudeObject)
        gen.writeEndObject()
    }
}