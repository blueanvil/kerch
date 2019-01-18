package com.blueanvil.kerch.krude

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.annotation
import org.elasticsearch.action.search.SearchRequestBuilder
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class Krude<T : KrudeObject>(private val kerch: Kerch,
                             private val objectType: KClass<T>,
                             private val indexNameMapper: (String) -> String = { it }) {

    private val annotation: KrudeType

    init {
        val typeAnnotation = annotation(objectType, KrudeType::class)
                ?: throw IllegalStateException("Class $objectType is not annotated with @KrudeType")
        annotation = typeAnnotation
    }

    val index = indexNameMapper(annotation.index)

    fun get(id: String): T? {
        return kerch.search(index).get(id, objectType)
    }

    fun save(value: T): String {
        return kerch.indexer(index).index(value)
    }

    fun request(): SearchRequestBuilder {
        return kerch.search(index).request()
    }

    fun field(fieldName: String): String {
        return "${annotation.type}.$fieldName"
    }
}