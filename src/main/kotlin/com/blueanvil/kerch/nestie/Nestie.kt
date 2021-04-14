package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.annotation
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.elasticsearch.client.RestHighLevelClient
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation

/**
 * @author Cosmin Marginean
 */
class Nestie(esClient: RestHighLevelClient,
             packages: Collection<String>) {

    constructor(nodes: Collection<String>,
                packages: Collection<String>) : this(Kerch.restClient(nodes), packages)

    val typesToClasses: Map<String, KClass<out Any>>
    val classesToAnontations: Map<KClass<out Any>, NestieDoc>
    val objectMapper: ObjectMapper = jacksonObjectMapper()

    val kerch = Kerch(esClient = esClient,
            toDocument = { json, _ -> toDocument(json) },
            toJson = { document -> toJson(document) })

    init {
        val module = SimpleModule()
        typesToClasses = mutableMapOf()
        classesToAnontations = mutableMapOf()

        val reflections = reflections(packages)
        reflections.getTypesAnnotatedWith(NestieDoc::class.java)
                .forEach { docClass ->
                    val annotation = docClass.kotlin.findAnnotation<NestieDoc>()
                    if (annotation != null) {
                        log.info("Found Document $docClass with type '${annotation.type}'")
                        typesToClasses[annotation.type] = docClass.kotlin
                        classesToAnontations[docClass.kotlin] = annotation
                        reflections.getSubTypesOf(docClass).forEach {
                            classesToAnontations[it.kotlin] = annotation
                        }
                    }
                }

        module.addSerializer(DocWrapper::class.javaObjectType, Serializer(objectMapper, classesToAnontations))
        module.addDeserializer(DocWrapper::class.javaObjectType, Deserializer(objectMapper, typesToClasses))
        objectMapper.registerModule(module)
    }

    fun <T : Any> store(docType: KClass<T>, index: String, indexMapper: (String) -> String = { it }): NestieIndexStore<T> {
        return NestieIndexStore(kerch, index, docType, indexMapper)
    }

    internal fun <T : Any> toDocument(json: String): T {
        return objectMapper.readValue(json, DocWrapper::class.javaObjectType).document as T
    }

    internal fun toJson(value: Any): String {
        return objectMapper.writeValueAsString(DocWrapper(value))
    }

    private fun reflections(packages: Collection<String>): Reflections {
        val config = ConfigurationBuilder()
        packages.forEach { config.addUrls(ClasspathHelper.forPackage(it)) }
        config.setScanners(TypeAnnotationsScanner(), SubTypesScanner())
        Reflections.log = null
        return Reflections(config)
    }

    companion object {
        private val log = LoggerFactory.getLogger(Nestie::class.java)

        internal fun <T : Any> annotation(objectType: KClass<T>): NestieDoc {
            return annotation(objectType, NestieDoc::class)
                    ?: throw IllegalStateException("Class $objectType is not annotated with @DocType")
        }

        fun <T : Any> field(objectType: KClass<T>, fieldName: String) = "${annotation(objectType).type}.$fieldName"
    }
}