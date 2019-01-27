package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.nestie.model.Paragraph
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider

/**
 * @author Cosmin Marginean
 */
class ParagraphSerializer : JsonSerializer<Paragraph>() {

    override fun serialize(value: Paragraph, gen: JsonGenerator, serializers: SerializerProvider?) {
        gen.writeString("${value.length}:${value.text}")
    }
}