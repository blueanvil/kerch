package com.blueanvil.kerch.krude

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * @author Cosmin Marginean
 */
class DogDeserializer : JsonDeserializer<Dog>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Dog {
        return Dog(p.valueAsString.replace(".nothing", ""))
    }
}