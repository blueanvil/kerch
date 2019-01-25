package com.blueanvil.kerch.krude

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.allHits
import com.blueanvil.kerch.batch.DocumentBatch
import com.blueanvil.kerch.index.Indexer
import com.blueanvil.kerch.search.KerchRequest
import com.blueanvil.kerch.search.Search
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
open class Krude<T : KrudeObject>(private val kerch: Kerch,
                                  private val objectType: KClass<T>,
                                  private val indexNameMapper: (String) -> String = { it }) {

    private val annotation: KrudeType = Krudes.annotation(objectType)

    val index: String get() = indexNameMapper(annotation.index)
    val indexer: Indexer get() = kerch.indexer(index)
    val search: Search get() = kerch.search(index)

    fun get(id: String): T? {
        return search.get(id, objectType)
    }

    fun save(value: T, waitRefresh: Boolean = false): String {
        return indexer.index(value, waitRefresh)
    }

    fun request(): KerchRequest<T> {
        return search.request(objectType)
    }

    fun field(fieldName: String): String {
        return "${annotation.type}.$fieldName"
    }

    fun findAll(): Sequence<T> {
        return find(QueryBuilders.existsQuery(field("id")))
    }

    fun find(query: QueryBuilder): Sequence<T> {
        return request()
                .setQuery(query)
                .allHits()
                .map { kerch.document(it, objectType) }
    }

    fun batch(afterIndex: ((Collection<T>) -> Unit)? = null): DocumentBatch<T> {
        return indexer.batch(afterIndex = afterIndex)
    }
}