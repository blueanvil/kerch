package com.blueanvil.kerch.krude

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * @author Cosmin Marginean
 */
class CustomIntDeserializer : JsonDeserializer<Int>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Int {
        return p.valueAsString.replace("nothing", "").toInt()
    }
}