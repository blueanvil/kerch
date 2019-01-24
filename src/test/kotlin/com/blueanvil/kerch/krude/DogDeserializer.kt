package com.blueanvil.kerch.krude

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer

/**
 * @author Cosmin Marginean
 */
class DogDeserializer : JsonDeserializer<BottomDog>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): BottomDog {
        return BottomDog(p.valueAsString.replace(".nothing", ""))
    }
}