package com.blueanvil.kerch.krude

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.annotation
import com.blueanvil.kerch.krude.json.KrudeObjectDeserializer
import com.blueanvil.kerch.reflections
import com.blueanvil.krude.json.KrudeObjectSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * @author Cosmin Marginean
 */
class Krudes(private val kerch: Kerch,
             packages: Collection<String>,
             private val indexNameMapper: (String) -> String = { it }) {

    private val typesToClasses: MutableMap<String, Class<out KrudeObject>> = HashMap()

    init {
        val module = SimpleModule()

        val reflections = reflections(packages)
        reflections.getSubTypesOf(KrudeObject::class.java)
                .forEach { krudeObjectClass ->
                    val annotation = krudeObjectClass.kotlin.findAnnotation<KrudeType>()
                    if (annotation != null) {
                        log.info("Found KrudeObject $krudeObjectClass with index '${annotation.index}' and type '${annotation.type}'")
                        typesToClasses[annotation.type] = krudeObjectClass
                    }
//                    val deserializer = KrudeObjectDeserializer(krudeObjectClass.kotlin as KClass<KrudeObject>)
//                    module.addDeserializer(krudeObjectClass.kotlin.javaObjectType as Class<Any>, deserializer)
                }

        module.setSerializerModifier(KrudeObjectSerializer.Modifier())
        module.setDeserializerModifier(KrudeObjectDeserializer.Modifier())
        kerch.addSerializationModule(module)
    }

    fun <T : KrudeObject> forType(objectType: KClass<T>): Krude<T> {
        return Krude(kerch, objectType, indexNameMapper)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Krudes::class.java)

        internal fun <T : KrudeObject> annotation(objectType: KClass<T>): KrudeType {
            return annotation(objectType, KrudeType::class)
                    ?: throw IllegalStateException("Class $objectType is not annotated with @KrudeType")
        }

        fun <T : KrudeObject> field(objectType: KClass<T>, fieldName: String) = "${annotation(objectType).type}.$fieldName"
    }
}