package com.blueanvil.krude.json

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.Krudes
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.jsontype.TypeSerializer
import java.lang.reflect.ParameterizedType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaType


/**
 * @author Cosmin Marginean
 */
class KrudeObjectSerializer : JsonSerializer<KrudeObject>() {

    override fun serialize(value: KrudeObject, gen: JsonGenerator, serializers: SerializerProvider) {
        gen.writeStartObject()
        gen.writeFieldName(Krudes.annotation(value::class).type)

        gen.writeStartObject()
        writeProps(value, gen, serializers)
        gen.writeEndObject()
        gen.writeEndObject()
    }

    override fun serializeWithType(value: KrudeObject, gen: JsonGenerator, serializers: SerializerProvider, typeSer: TypeSerializer) {
        val typeId = typeSer.typeId(value, JsonToken.START_OBJECT)

        typeSer.writeTypePrefix(gen, typeId)

        gen.writeFieldName(Krudes.annotation(value::class).type)
        gen.writeStartObject()
        writeProps(value, gen, serializers)
        gen.writeEndObject()
        typeSer.writeTypeSuffix(gen, typeId)
    }

    private fun writeProps(value: KrudeObject, gen: JsonGenerator, serializers: SerializerProvider) {
        value.javaClass.kotlin.memberProperties.forEach {
            println("PROP:${it.name}")
            gen.writeFieldName(it.name)
            val propValue = it.get(value)

            val javaType = it.returnType.javaType
            if (javaType is ParameterizedType) {
                serializers.findValueSerializer(javaType.rawType as Class<*>).serialize(propValue, gen, serializers)
            } else {
                serializers.findValueSerializer(javaType as Class<*>).serialize(propValue, gen, serializers)
            }
        }
    }
}
