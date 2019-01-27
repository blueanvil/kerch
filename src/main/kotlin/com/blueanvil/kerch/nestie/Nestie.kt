package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.*
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
class Nestie(esClient: Client,
             packages: Collection<String>,
             defaultType: String = Kerch.TYPE) {

    constructor(clusterName: String,
                nodes: Collection<String>,
                packages: Collection<String>,
                defaultType: String = Kerch.TYPE) : this(Kerch.transportClient(clusterName, nodes), packages, defaultType)

    private val typesToClasses: MutableMap<String, KClass<out Document>> = HashMap()
    private val classesToAnontations: MutableMap<KClass<out Document>, DocType> = HashMap()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    internal val kerch = Kerch(esClient = esClient,
            defaultType = defaultType,
            toDocument = { json, _ -> toDocument(json) },
            toJson = { document -> toJson(document) })

    init {
        val module = SimpleModule()

        val reflections = reflections(packages)
        reflections.getSubTypesOf(Document::class.java)
                .forEach { docClass ->
                    val annotation = docClass.kotlin.findAnnotation<DocType>()
                    if (annotation != null) {
                        log.info("Found Document $docClass with index '${annotation.index}' and type '${annotation.type}'")
                        typesToClasses[annotation.type] = docClass.kotlin
                        classesToAnontations[docClass.kotlin] = annotation
                        reflections.getSubTypesOf(docClass).forEach {
                            classesToAnontations[it.kotlin] = annotation
                        }
                    }
                }

        module.addSerializer(DocWrapper::class.javaObjectType, Serializer(objectMapper, classesToAnontations))
        module.addDeserializer(DocWrapper::class.javaObjectType, Deserializer(objectMapper, typesToClasses))
        addSerializationModule(module)
    }

    fun <T : Document> store(docType: KClass<T>): TypedIndexStore<T> {
        return TypedIndexStore(kerch, classesToAnontations[docType]!!.index, docType)
    }

    fun addSerializationModule(module: Module): Nestie {
        objectMapper.registerModule(module)
        return this
    }

    internal fun <T : Document> toDocument(json: String): T {
        return objectMapper.readValue(json, DocWrapper::class.javaObjectType).document as T
    }

    internal fun toJson(value: Document): String {
        return objectMapper.writeValueAsString(DocWrapper(value))
    }

    companion object {
        private val log = LoggerFactory.getLogger(Nestie::class.java)

        internal fun <T : Document> annotation(objectType: KClass<T>): DocType {
            return annotation(objectType, DocType::class)
                    ?: throw IllegalStateException("Class $objectType is not annotated with @DocType")
        }

        fun <T : Document> field(objectType: KClass<T>, fieldName: String) = "${annotation(objectType).type}.$fieldName"
    }
}