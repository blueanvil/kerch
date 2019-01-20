package com.blueanvil.kerch.krude.json

import com.blueanvil.kerch.krude.KrudeObject
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
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
                val paramValue = topNode.get(param.name)
                if (paramValue != null) {
                    mapper.readValue(paramValue.toString(), typeToRead(param.type.javaType) as Class<*>)
                } else {
                    null
                }
            }.toTypedArray()
            primaryConstructor.call(*args)
        } else {
            objectType.kotlin.createInstance()
        }

        objectType.kotlin.memberProperties
                .filter { !handledProps.contains(it.name) }
                .forEach { property ->
                    val paramValueNode = topNode.get(property.name)
                    val value = if (paramValueNode != null) {
                        mapper.readValue(paramValueNode.toString(), typeToRead(property.returnType.javaType) as Class<*>)
                    } else {
                        null
                    }
                    if (property is KMutableProperty<*>) {
                        property.setter.call(krudeObject, value)
                    }
                }
        return krudeObject
    }

    private fun typeToRead(javaType: Type): Type {
        return if (javaType is ParameterizedType) {
            javaType.rawType
        } else {
            javaType
        }
    }
}
