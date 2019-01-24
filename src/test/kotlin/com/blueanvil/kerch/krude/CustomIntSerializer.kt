package com.blueanvil.kerch.krude

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * @author Cosmin Marginean
 */
class CustomIntSerializer : JsonSerializer<Int>() {

    override fun serialize(value: Int?, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString(value.toString() + "nothing")
    }
}