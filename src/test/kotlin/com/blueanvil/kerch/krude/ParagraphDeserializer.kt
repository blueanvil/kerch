package com.blueanvil.kerch.krude

import com.blueanvil.kerch.krude.model.Paragraph
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