package com.blueanvil.krude.json

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.Krudes
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializationConfig
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier


/**
 * @author Cosmin Marginean
 */
class KrudeObjectSerializer(private val defaultSerializer: JsonSerializer<KrudeObject>) : JsonSerializer<KrudeObject>() {

    override fun serialize(value: KrudeObject, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName(Krudes.annotation(value::class).type)
        defaultSerializer.serialize(value, gen, serializers)
        gen.writeEndObject()
    }

    override fun serializeWithType(value: KrudeObject, gen: JsonGenerator, serializers: SerializerProvider, typeSer: TypeSerializer) {
        gen.writeStartObject()
        gen.writeFieldName(Krudes.annotation(value::class).type)
        defaultSerializer.serializeWithType(value, gen, serializers, typeSer)
        gen.writeEndObject()
    }

    class Modifier : BeanSerializerModifier() {
        override fun modifySerializer(config: SerializationConfig?, beanDesc: BeanDescription, serializer: JsonSerializer<*>): JsonSerializer<*> {
            return if (KrudeObject::class.java.isAssignableFrom(beanDesc.beanClass)) {
                KrudeObjectSerializer(serializer as JsonSerializer<KrudeObject>)
            } else {
                super.modifySerializer(config, beanDesc, serializer)
            }
        }
    }
}
