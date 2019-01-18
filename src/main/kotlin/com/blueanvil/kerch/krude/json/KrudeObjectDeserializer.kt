package com.redpowder.storage

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.Krudes
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

/**
 * @author Cosmin Marginean
 */
class KrudeObjectDeserializer(private val krudes: Krudes) : JsonDeserializer<KrudeObject>() {

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): KrudeObject? {
        val mapper = parser.getCodec() as ObjectMapper
        val treeNode = parser.codec.readTree<TreeNode>(parser)
        val type = treeNode.fieldNames().next()
        val objectType = krudes.classForType(type)
        val topNode = treeNode.get(type)

        val primaryConstructor = objectType.kotlin.primaryConstructor
        val handledProps: MutableSet<String> = hashSetOf()
        val krudeObject = if (primaryConstructor != null) {
            val args = primaryConstructor.parameters.map { param ->
                handledProps.add(param.name!!)
                mapper.readValue(topNode.get(param.name).toString(), param.type.javaType as Class<*>)
            }.toTypedArray()
            primaryConstructor.call(*args)
        } else {
            objectType.kotlin.createInstance()
        }

        objectType.kotlin.memberProperties
                .filter { !handledProps.contains(it.name) }
                .forEach { property ->
                    val value = mapper.readValue(topNode.get(property.name).toString(), property.returnType.javaType as Class<*>)
                    if (property is KMutableProperty<*>) {
                        property.setter.call(krudeObject, value)
                    }
                }
        return krudeObject
    }
}
