package com.blueanvil.kerch

import com.blueanvil.kerch.batch.DocumentBatch
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilder
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
open class TypedIndexStore<T : ElasticsearchDocument>(kerch: Kerch,
                                                      index: String,
                                                      private val docType: KClass<T>,
                                                      indexMapper: (String) -> String,
                                                      private val adaptQuery: (QueryBuilder) -> QueryBuilder = { it }) : IndexStoreBase<T>(kerch, index, indexMapper) {

    fun get(id: String): T? {
        val response = doGet(id)
        if (!response.isExists) {
            return null
        }
        return kerch.document(response.sourceAsString, response.seqNo, docType)
    }

    fun save(doc: T, waitRefresh: Boolean = true): String {
        return index(doc, waitRefresh)
    }

    fun docBatch(size: Int = 100,
                 waitRefresh: Boolean = false,
                 afterIndex: ((Collection<T>) -> Unit)? = null): DocumentBatch<T> {
        return DocumentBatch(this, size, waitRefresh, afterIndex)
    }

    override fun search(request: SearchRequest): List<T> {
        request.query(adaptQuery(request.source().query()))
        return kerch.esClient.search(request, RequestOptions.DEFAULT).hits.hits.map { kerch.toDocument(it.sourceAsString, docType) as T }
    }

    override fun scroll(request: SearchRequest): Sequence<T> {
        request.query(adaptQuery(request.source().query()))
        return doScroll(request).map { kerch.toDocument(it.sourceAsString, docType) as T }
    }

    override fun count(query: QueryBuilder): Long {
        return super.count(adaptQuery(query))
    }

    override fun allIds(request: SearchRequest): Sequence<String> {
        return super.allIds(request.query(adaptQuery(request.source().query())))
    }
}