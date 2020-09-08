package com.blueanvil.kerch.nestie

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class Serializer(private val objectMapper: ObjectMapper,
                 private val classesToAnnotations: Map<KClass<out Any>, NestieDoc>) : JsonSerializer<DocWrapper>() {

    override fun serialize(value: DocWrapper, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeStartObject()
        gen.writeFieldName(classesToAnnotations[value.document::class]!!.type)
        objectMapper.writeValue(gen, value.document)
        gen.writeEndObject()
    }
}