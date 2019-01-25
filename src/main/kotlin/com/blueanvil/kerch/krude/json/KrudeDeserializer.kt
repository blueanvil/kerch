package com.blueanvil.kerch.krude.json

import com.blueanvil.kerch.krude.KrudeObjectWrapper
import com.blueanvil.kerch.krude.Krudes
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * @author Cosmin Marginean
 */
class KrudeDeserializer(private val objectMapper: ObjectMapper,
                        private val krudes: Krudes) : JsonDeserializer<KrudeObjectWrapper>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): KrudeObjectWrapper {
        val tree = p.readValueAsTree<TreeNode>() as ObjectNode
        val type = tree.fieldNames().next()
        val topNode = tree.get(type)
        val toString = topNode.toString()
        return KrudeObjectWrapper(objectMapper.readValue(toString, krudes.typesToClasses[type]))
    }
}