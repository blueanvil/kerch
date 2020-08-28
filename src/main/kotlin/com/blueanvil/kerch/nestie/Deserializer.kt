package com.blueanvil.kerch.nestie

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class Deserializer(private val objectMapper: ObjectMapper,
                   private val typesToClasses: Map<String, KClass<out Any>>) : JsonDeserializer<DocWrapper>() {

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): DocWrapper {
        val tree = p.readValueAsTree<TreeNode>() as ObjectNode
        val type = tree.fieldNames().next()
        val topNode = tree.get(type)
        val toString = topNode.toString()
        return DocWrapper(objectMapper.readValue(toString, typesToClasses[type]!!.javaObjectType))
    }
}