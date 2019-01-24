package com.blueanvil.kerch.krude

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * @author Cosmin Marginean
 */
class DogSerializer : JsonSerializer<BottomDog>() {

    override fun serialize(value: BottomDog?, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString(value?.name + ".nothing")
    }
}