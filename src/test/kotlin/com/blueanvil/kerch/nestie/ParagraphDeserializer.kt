package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.nestie.model.Paragraph
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * @author Cosmin Marginean
 */
class ParagraphDeserializer : JsonDeserializer<Paragraph>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Paragraph {
        val elements = p.valueAsString.split(":").toList()
        return Paragraph(elements[0].toInt(), elements[1])
    }
}