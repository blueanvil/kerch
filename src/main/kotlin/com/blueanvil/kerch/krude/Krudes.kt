package com.blueanvil.kerch.krude

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.annotation
import com.blueanvil.kerch.krude.json.KrudeObjectDeserializer
import com.blueanvil.kerch.reflections
import com.blueanvil.krude.json.KrudeObjectSerializer
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.module.SimpleModule
import org.elasticsearch.client.Client
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * @author Cosmin Marginean
 */
class Krudes(esClient: Client,
             packages: Collection<String>,
             private val indexNameMapper: (String) -> String = { it },
             defaultType: String = Kerch.TYPE) {

    constructor(clusterName: String,
                nodes: Collection<String>,
                packages: Collection<String>,
                indexNameMapper: (String) -> String = { it },
                defaultType: String = Kerch.TYPE) : this(Kerch.transportClient(clusterName, nodes), packages, indexNameMapper, defaultType)

    private val typesToClasses: MutableMap<String, Class<out KrudeObject>> = HashMap()
    val kerch = Kerch(esClient, defaultType)

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
                }

        module.setSerializerModifier(KrudeObjectSerializer.Modifier())
        module.setDeserializerModifier(KrudeObjectDeserializer.Modifier())
        kerch.addSerializationModule(module)
    }

    fun addSerializationModule(module: Module): Krudes {
        kerch.addSerializationModule(module)
        return this
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