package com.blueanvil.kerch.krude.json

import com.blueanvil.kerch.krude.KrudeObject
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import com.fasterxml.jackson.databind.deser.ResolvableDeserializer
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer

/**
 * @author Cosmin Marginean
 */
class KrudeObjectDeserializer(private val defaultDeserializer: JsonDeserializer<KrudeObject>) : JsonDeserializer<KrudeObject>(), ResolvableDeserializer {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): KrudeObject? {
        if (p.currentName == null) {
            p.nextToken()
            p.nextToken()
        }
        return defaultDeserializer.deserialize(p, ctxt)
    }

    override fun deserializeWithType(p: JsonParser, ctxt: DeserializationContext, typeDeserializer: TypeDeserializer?): Any {
        p.nextToken()
        p.nextToken()
        return defaultDeserializer.deserializeWithType(p, ctxt, typeDeserializer)
    }

    override fun resolve(ctxt: DeserializationContext?) {
        if (defaultDeserializer is ResolvableDeserializer) {
            (defaultDeserializer as ResolvableDeserializer).resolve(ctxt)
        }
    }

    class Modifier : BeanDeserializerModifier() {

        override fun modifyDeserializer(config: DeserializationConfig?, beanDesc: BeanDescription, deserializer: JsonDeserializer<*>?): JsonDeserializer<*> {
            return if (KrudeObject::class.java.isAssignableFrom(beanDesc.beanClass)) {
                KrudeObjectDeserializer(deserializer as JsonDeserializer<KrudeObject>)
            } else {
                return super.modifyDeserializer(config, beanDesc, deserializer)
            }
        }
    }
}
