package com.blueanvil.kerch.krude

import com.blueanvil.kerch.krude.model.Paragraph
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