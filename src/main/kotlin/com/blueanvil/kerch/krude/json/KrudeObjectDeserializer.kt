package com.blueanvil.kerch.krude.json

import com.blueanvil.kerch.krude.KrudeObject
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaType

/**
 * @author Cosmin Marginean
 */
class KrudeObjectDeserializer(private val krudeObjectType: KClass<KrudeObject>) : JsonDeserializer<KrudeObject>() {

    override fun deserialize(parser: JsonParser, ctxt: DeserializationContext): KrudeObject? {
        val mapper = parser.codec as ObjectMapper
        val treeNode = parser.codec.readTree<TreeNode>(parser)
        val type = treeNode.fieldNames().next()

        return readObject(krudeObjectType.javaObjectType, mapper, treeNode.get(type))
    }

    private fun readObject(objectType: Class<out KrudeObject>, mapper: ObjectMapper, topNode: TreeNode): KrudeObject {
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
