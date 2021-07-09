package com.blueanvil.kerch

import com.blueanvil.kerch.batch.DocumentBatch
import com.blueanvil.kerch.batch.RawIndexBatch
import com.blueanvil.kerch.error.IndexError
import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.ClearScrollRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.core.CountRequest
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders.matchAllQuery
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.index.reindex.UpdateByQueryRequest
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.sort.SortBuilder
import org.elasticsearch.search.sort.SortBuilders
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class IndexStore(protected val kerch: Kerch,
                 private val index: String,
                 private val indexMapper: (String) -> String) {

    companion object {
        private val log = LoggerFactory.getLogger(IndexStore::class.java)
    }

    val indexName: String get() = indexMapper(index)

    fun <T : Any> get(id: String, documentType: KClass<T>): T? {
        val response = rawGet(id)
        if (!response.isExists) {
            return null
        }
        return kerch.document(response.sourceAsString, response.seqNo, documentType)
    }

    fun <T : Any> scroll(docType: KClass<T>,
                         query: QueryBuilder = matchAllQuery(),
                         pageSize: Int = 100,
                         keepAlive: TimeValue = TimeValue.timeValueMinutes(10),
                         sort: SortBuilder<*> = SortBuilders.fieldSort("_id")): Sequence<T> {
        return scroll(query, pageSize, keepAlive, sort).map { kerch.document(it.sourceAsString, it.seqNo, docType) }
    }

    fun scroll(query: QueryBuilder = matchAllQuery(),
               pageSize: Int = 100,
               keepAlive: TimeValue = TimeValue.timeValueMinutes(10),
               sort: SortBuilder<*> = SortBuilders.fieldSort("_id")): Sequence<SearchHit> {

        val request = searchRequest()
                .query(query)
                .paging(0, pageSize)
                .sort(sort)
        return doScroll(request, keepAlive)
    }

    fun exists(id: String): Boolean {
        return rawGet(id).isExists
    }

    fun rawGet(id: String): GetResponse {
        val request = GetRequest(indexName, id)
        return kerch.esClient.get(request, RequestOptions.DEFAULT)
    }

    fun searchRequest(): SearchRequest = SearchRequest(indexName)
            .query(matchAllQuery())
            .paging(0, 10)

    fun search(request: SearchRequest): KerchSearchResponse<SearchHit> {
        val hits = rawSearch(request).hits
        return KerchSearchResponse(hits.totalHits.value, hits.hits.toList())
    }

    fun <T : Any> search(request: SearchRequest, documentType: KClass<T>): List<T> {
        return rawSearch(request).hits.hits
                .toList()
                .map { hit -> kerch.document(hit.sourceAsString, hit.seqNo, documentType) }
    }

    fun rawSearch(request: SearchRequest): SearchResponse {
        return kerch.esClient.search(request, RequestOptions.DEFAULT)
    }

    fun search(request: SearchRequest, outputStream: OutputStream) {
        val printStream = PrintStream(outputStream)
        printStream.print(rawSearch(request).toString())
        printStream.close()
    }

    fun allIds(query: QueryBuilder = matchAllQuery()): Sequence<String> {
        val request = searchRequest()
                .query(query)
                .paging(0, 100)
        return doScroll(request)
                .map { it.id }
    }

    fun count(query: QueryBuilder = matchAllQuery()): Long {
        return kerch.esClient.count(CountRequest(indexName).query(query), RequestOptions.DEFAULT).count
    }

    fun delete(id: String, waitRefresh: Boolean = false) {
        val request = DeleteRequest(indexName).id(id)
        if (waitRefresh) {
            request.refreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL
        }
        kerch.esClient.delete(request, RequestOptions.DEFAULT)
    }

    fun delete(query: QueryBuilder) {
        val request = DeleteByQueryRequest(indexName).setQuery(query)
        kerch.esClient.deleteByQuery(request, RequestOptions.DEFAULT)
    }

    fun batch(size: Int = 100,
              waitRefresh: Boolean = false,
              afterEachBulkIndex: ((Collection<Pair<String, String>>) -> Unit)? = null): RawIndexBatch {
        return RawIndexBatch(size, { docs -> index(docs, waitRefresh) }, afterEachBulkIndex)
    }

    fun <T : Any> docBatch(size: Int = 100,
                           waitRefresh: Boolean = false,
                           afterEachBulkIndex: ((Collection<T>) -> Unit)? = null): DocumentBatch<T> {
        return DocumentBatch(size, { docs -> index(docs, waitRefresh) }, afterEachBulkIndex)
    }

    fun index(documents: Collection<Any>, waitRefresh: Boolean = false) {
        val documentsMap = documents
                .map { it.documentId to kerch.toJsonString(it) }
                .toMap()
        this.index(documentsMap, waitRefresh)
    }

    fun findOne(query: QueryBuilder, sort: SortBuilder<*>? = null): SearchHit? {
        val request = searchRequest()
                .query(query)
                .paging(0, 1)
        if (sort != null) {
            request.sort(sort)
        }
        return search(request).hits.firstOrNull()
    }

    fun <T : Any> findOne(query: QueryBuilder, documentType: KClass<T>, sort: SortBuilder<*>? = null): T? {
        val hit = findOne(query, sort)
        return if (hit != null) kerch.document(hit.sourceAsString, hit.seqNo, documentType) else null
    }

    fun updateField(documentId: String, field: String, value: Any?, waitRefresh: Boolean = false) {
        val doc = jsonBuilder().startObject().field(field, value).endObject()
        val request = UpdateRequest(indexName, documentId).doc(doc)
        if (waitRefresh)
            request.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        kerch.esClient.update(request, RequestOptions.DEFAULT)
    }

    fun updateWithPainlessScript(documentId: String, script: String, params: Map<String, Any?>, waitRefresh: Boolean = false, retryOnConflict: Int = 0) {
        val request = UpdateRequest(indexName, documentId)
                .script(Script(ScriptType.INLINE, "painless", script, params))
                .retryOnConflict(retryOnConflict)
        if (waitRefresh)
            request.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        kerch.esClient.update(request, RequestOptions.DEFAULT)
    }

    fun updateByQuery(query: QueryBuilder, script: String, params: Map<String, Any?>, timeout: TimeValue? = null) {
        val request = UpdateByQueryRequest(indexName)
                .setQuery(query)
                .setScript(Script(ScriptType.INLINE, "painless", script, params))

        if (timeout != null) {
            request.timeout = timeout
        }

        kerch.esClient.updateByQuery(request, RequestOptions.DEFAULT)
    }

    fun refreshStore() {
        kerch.esClient.indices().refresh(RefreshRequest(indexName), RequestOptions.DEFAULT)
    }

    @Throws(ActionRequestValidationException::class)
    fun indexRaw(id: String, jsonString: String, seqNo: Long = 0, waitRefresh: Boolean = false): String {
        var request = indexRequest(id, waitRefresh)
        if (seqNo > 0) {
            request.setIfSeqNo(seqNo)
        }
        request.source(jsonString, XContentType.JSON)
        val response = kerch.esClient.index(request, RequestOptions.DEFAULT)
        return response.id
    }

    @Throws(ActionRequestValidationException::class)
    fun index(document: Any, waitRefresh: Boolean = false): String {
        return indexRaw(document.documentId, kerch.toJsonString(document), document.sequenceNumber, waitRefresh)
    }

    @Throws(IndexError::class)
    fun index(documents: Map<String, String>,
              waitRefresh: Boolean = false) {
        val bulkRequest = BulkRequest()
        if (waitRefresh)
            bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL

        documents.forEach { (id, jsonDoc) ->
            bulkRequest.add(IndexRequest(indexName)
                    .id(id)
                    .source(jsonDoc, XContentType.JSON))
        }
        val response = kerch.esClient.bulk(bulkRequest, RequestOptions.DEFAULT)
        if (response.hasFailures())
            throw IndexError(response)
    }

    fun createIndex(shards: Int = 5) = kerch.admin.createIndex(indexName, shards)
    fun deleteIndex() = kerch.admin.deleteIndex(indexName)
    val indexExists: Boolean get() = kerch.admin.indexExists(indexName)
    var readOnly: Boolean
        get() = kerch.admin.isReadOnly(indexName)
        set(value) {
            kerch.admin.setReadOnly(indexName, value)
        }

    private fun indexRequest(id: String?, waitRefresh: Boolean = false): IndexRequest {
        var request = IndexRequest(indexName)
        if (id != null) {
            request = request.id(id)
        }
        if (waitRefresh) {
            request.refreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL
        }
        return request
    }

    internal fun doScroll(request: SearchRequest, keepAlive: TimeValue = TimeValue.timeValueMinutes(10)): Sequence<SearchHit> {
        val pageSize = request.source().size()

        var page = kerch.esClient.search(request.scroll(keepAlive), RequestOptions.DEFAULT)
        val totalHits = page.hits.totalHits.value
        var crtIndex = 0
        var scrollId = page.scrollId

        return generateSequence {
            var hit: SearchHit? = null
            if (totalHits > 0 && crtIndex < totalHits) {
                var crtPageIndex = crtIndex % pageSize
                hit = page.hits.hits[crtPageIndex]
                crtIndex++
                if (crtIndex % pageSize == 0) {
                    page = kerch.esClient.scroll(SearchScrollRequest(scrollId).scroll(keepAlive), RequestOptions.DEFAULT)
                    scrollId = page.scrollId
                }
            }

            if (hit == null) {
                clearScroll(scrollId)
            }

            hit
        }
    }

    private fun clearScroll(scrollId: String?) {
        val request = ClearScrollRequest()
        request.addScrollId(scrollId)
        val response = kerch.esClient.clearScroll(request, RequestOptions.DEFAULT)
        if (!response.isSucceeded) {
            log.error("Could not close scroll $scrollId")
            throw RuntimeException("Could not close scroll $scrollId")
        }

        log.debug("Freed ${response.numFreed} scrolls (scroll ID: $scrollId)")
    }
}

data class KerchSearchResponse<T : Any>(val totalHits: Long,
                                        val hits: List<T>)