package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.batch.DocumentBatch
import com.blueanvil.kerch.paging
import com.blueanvil.kerch.query
import com.blueanvil.kerch.sort
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.unit.TimeValue
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
class NestieIndexStore<T : Any>(private val kerch: Kerch,
                                private val index: String,
                                private val docType: KClass<T>,
                                indexMapper: (String) -> String) {

    private val rawStore = kerch.store(index, indexMapper)
    private val esDocType = Nestie.annotation(docType).type

    val indexName: String get() = rawStore.indexName

    fun get(id: String): T? {
        val response = rawStore.rawGet(id)
        if (!response.isExists) {
            return null
        }
        return kerch.document(response.sourceAsString, response.version, docType)
    }

    fun exists(id: String): Boolean = rawStore.exists(id)
    fun delete(id: String, waitRefresh: Boolean = false) = rawStore.delete(id, waitRefresh)
    fun delete(query: QueryBuilder) = rawStore.delete(query.wrap())

    fun save(doc: T, waitRefresh: Boolean = false): String {
        return rawStore.index(doc, waitRefresh)
    }

    fun docBatch(size: Int = 100,
                 waitRefresh: Boolean = false,
                 afterEachBulkIndex: ((Collection<T>) -> Unit)? = null): DocumentBatch<T> {
        return DocumentBatch(size, { docs -> rawStore.index(docs, waitRefresh) }, afterEachBulkIndex)
    }

    fun findOne(query: QueryBuilder, sort: SortBuilder<*>? = null): T? {
        val request = searchRequest()
                .query(query.wrap())
                .paging(0, 1)
        if (sort != null) {
            request.sort(sort)
        }
        return search(request).firstOrNull()
    }

    fun findOne(query: QueryBuilder, sortField: String, sortOder: SortOrder): T? {
        return findOne(query, SortBuilders.fieldSort(sortField).order(sortOder))
    }

    fun searchRequest(): SearchRequest = rawStore.searchRequest()

    fun search(request: SearchRequest = searchRequest()): List<T> {
        return kerch.esClient
                .search(request.wrap(), RequestOptions.DEFAULT)
                .hits
                .hits
                .map { kerch.toDocument(it.sourceAsString, docType) as T }
    }

    fun scroll(query: QueryBuilder = matchAllQuery(),
               pageSize: Int = 100,
               keepAlive: TimeValue = TimeValue.timeValueMinutes(10),
               sort: SortBuilder<*> = SortBuilders.fieldSort("_id")): Sequence<T> {
        val request = searchRequest()
                .query(query)
                .paging(0, pageSize)
                .sort(sort)
        return rawStore
                .doScroll(request.wrap(), keepAlive)
                .map { kerch.toDocument(it.sourceAsString, docType) as T }
    }

    fun updateField(documentId: String, nestieField: String, value: Any?, waitRefresh: Boolean = false) {
        val field = nestieField.replace(esDocType, "[\"${esDocType}\"]")
        rawStore.updateWithPainlessScript(documentId = documentId,
                script = """
                            ctx._source$field = params.newValue
                         """,
                params = mapOf("newValue" to value),
                waitRefresh = waitRefresh)
    }

    fun updateWithPainlessScript(documentId: String, script: String, params: Map<String, Any?>, waitRefresh: Boolean = false, retryOnConflict: Int = 0) {
        rawStore.updateWithPainlessScript(documentId, script, params, waitRefresh, retryOnConflict)
    }

    fun updateByQuery(query: QueryBuilder, script: String, params: Map<String, Any?>) {
        rawStore.updateByQuery(query, script, params)
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

    fun createIndex(shards: Int = 5) = rawStore.createIndex(shards)
    fun deleteIndex() = rawStore.deleteIndex()
    val indexExists: Boolean = rawStore.indexExists
    var readOnly: Boolean
        get() = rawStore.readOnly
        set(value) {
            rawStore.readOnly = value
        }
}
