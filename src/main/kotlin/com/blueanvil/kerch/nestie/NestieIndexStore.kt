package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.*
import com.blueanvil.kerch.batch.DocumentBatch
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.*
import org.elasticsearch.search.sort.SortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import java.io.OutputStream
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class NestieIndexStore<T : ElasticsearchDocument>(private val kerch: Kerch,
                                                  private val index: String,
                                                  private val docType: KClass<T>,
                                                  indexMapper: (String) -> String) {

    private val rawStore = kerch.store(index, indexMapper)

    val indexName: String get() = rawStore.indexName

    fun get(id: String): T? {
        val response = rawStore.rawGet(id)
        if (!response.isExists) {
            return null
        }
        return kerch.document(response.sourceAsString, docType)
    }

    fun delete(id: String, waitRefresh: Boolean = false) = rawStore.delete(id, waitRefresh)
    fun delete(query: QueryBuilder) = rawStore.delete(query.wrap())
    fun deleteIndex() = rawStore.deleteIndex()

    fun save(doc: T, waitRefresh: Boolean = false): String {
        return rawStore.index(doc, waitRefresh)
    }

    fun docBatch(size: Int = 100,
                 waitRefresh: Boolean = false,
                 afterIndex: ((Collection<T>) -> Unit)? = null): DocumentBatch<T> {
        return DocumentBatch(size, { docs -> rawStore.index(docs, waitRefresh) }, afterIndex)
    }

    fun findOne(query: QueryBuilder): T? {
        val request = searchRequest()
                .query(query.wrap())
                .paging(0, 1)
        return search(request).firstOrNull()
    }

    fun findOne(query: QueryBuilder, sort: SortBuilder<*>): T? {
        val request = searchRequest()
                .query(query.wrap())
                .paging(0, 1)
                .sort(sort)
        return search(request).firstOrNull()
    }

    fun findOne(query: QueryBuilder, sortField: String, sortOder: SortOrder): T? {
        return findOne(query, SortBuilders.fieldSort(sortField).order(sortOder))
    }

    fun searchRequest(): SearchRequest = rawStore.searchRequest()

    fun createIndex() {
        rawStore.createIndex()
    }

    fun search(request: SearchRequest = searchRequest()): List<T> {
        return kerch.esClient
                .search(request.wrap(), RequestOptions.DEFAULT)
                .hits
                .hits
                .map { kerch.toDocument(it.sourceAsString, docType) as T }
    }

    fun scroll(request: SearchRequest = searchRequest()): Sequence<T> {
        return rawStore
                .doScroll(request.wrap())
                .map { kerch.toDocument(it.sourceAsString, docType) as T }
    }

    fun search(request: SearchRequest, outputStream: OutputStream) = rawStore.search(request.wrap(), outputStream)

    fun count(query: QueryBuilder = matchAllQuery()): Long {
        return rawStore.count(query.wrap())
    }

    fun allIds(query: QueryBuilder = matchAllQuery()): Sequence<String> {
        return rawStore.allIds(query.wrap())
    }

    private fun QueryBuilder.wrap(): QueryBuilder {
        return boolQuery()
                .must(existsQuery(Nestie.field(docType, "id")))
                .must(this)
    }

    private fun SearchRequest.wrap(): SearchRequest {
        return query(source().query().wrap())
    }
}
