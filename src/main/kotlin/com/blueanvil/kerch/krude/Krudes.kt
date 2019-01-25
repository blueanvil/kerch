package com.blueanvil.kerch.krude

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.annotation
import com.blueanvil.kerch.krude.json.KrudeDeserializer
import com.blueanvil.kerch.krude.json.KrudeSerializer
import com.blueanvil.kerch.reflections
import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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

    internal val typesToClasses: MutableMap<String, Class<out KrudeObject>> = HashMap()
    internal val classesToTypes: MutableMap<Class<out KrudeObject>, String> = HashMap()
    internal val objectMapper: ObjectMapper = jacksonObjectMapper()

    internal val kerch = Kerch(esClient = esClient,
            defaultType = defaultType,
            toDocument = { json: String, _: KClass<out Document> ->
                toDocument(json)
            },
            toJson = { document: Document ->
                toJson(document as KrudeObject)
            })

    init {
        val module = SimpleModule()

        val reflections = reflections(packages)
        reflections.getSubTypesOf(KrudeObject::class.java)
                .forEach { krudeObjectClass ->
                    val annotation = krudeObjectClass.kotlin.findAnnotation<KrudeType>()
                    if (annotation != null) {
                        log.info("Found KrudeObject $krudeObjectClass with index '${annotation.index}' and type '${annotation.type}'")
                        typesToClasses[annotation.type] = krudeObjectClass
                        classesToTypes[krudeObjectClass] = annotation.type
                        reflections.getSubTypesOf(krudeObjectClass).forEach {
                            classesToTypes[it] = annotation.type
                        }
                    }
                }

        module.addSerializer(KrudeObjectWrapper::class.javaObjectType, KrudeSerializer(objectMapper, this))
        module.addDeserializer(KrudeObjectWrapper::class.javaObjectType, KrudeDeserializer(objectMapper, this))
        objectMapper.registerModule(module)
    }

    internal fun <T : KrudeObject> toDocument(json: String): T {
        return objectMapper.readValue(json, KrudeObjectWrapper::class.javaObjectType).krudeObject as T
    }

    internal fun toJson(value: KrudeObject): String {
        return objectMapper.writeValueAsString(KrudeObjectWrapper(value))
    }

    fun addSerializationModule(module: Module): Krudes {
        objectMapper.registerModule(module)
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