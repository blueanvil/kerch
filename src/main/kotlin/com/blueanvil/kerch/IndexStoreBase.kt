package com.blueanvil.kerch

import com.blueanvil.kerch.batch.IndexBatch
import com.blueanvil.kerch.error.IndexError
import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.bulk.BulkRequest
import org.elasticsearch.action.delete.DeleteRequest
import org.elasticsearch.action.get.GetRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexRequest
import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.action.search.SearchScrollRequest
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.action.update.UpdateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.core.CountRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentFactory.jsonBuilder
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.reindex.DeleteByQueryRequest
import org.elasticsearch.script.Script
import org.elasticsearch.script.ScriptType
import org.elasticsearch.search.SearchHit
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.slf4j.LoggerFactory
import java.io.OutputStream
import java.io.PrintStream

/**
 * @author Cosmin Marginean
 */
abstract class IndexStoreBase<T : Any>(protected val kerch: Kerch,
                                       private val index: String,
                                       private val indexMapper: (String) -> String) {

    val indexName: String get() = indexMapper(index)

    fun exists(id: String): Boolean {
        return rawGet(id).isExists
    }

    fun rawGet(id: String): GetResponse {
        val request = GetRequest(indexName, KerchConst.DEFAULTTYPE, id)
        return kerch.esClient.get(request, RequestOptions.DEFAULT)
    }

    fun searchRequest(): SearchRequest = SearchRequest(indexName).source(SearchSourceBuilder.searchSource().from(0).size(10).query(QueryBuilders.matchAllQuery()))

    fun rawSearch(request: SearchRequest): SearchResponse {
        return kerch.esClient.search(request, RequestOptions.DEFAULT)
    }

    abstract fun search(request: SearchRequest): List<T>

    abstract fun scroll(request: SearchRequest = searchRequest().size(100)): Sequence<T>

    fun ids(request: SearchRequest): List<String> {
        return kerch.esClient.search(request, RequestOptions.DEFAULT).hits.hits.map { it.id }
    }

    open fun allIds(request: SearchRequest = searchRequest()): Sequence<String> {
        return doScroll(request).map { it.id }
    }

    fun search(request: SearchRequest, outputStream: OutputStream) {
        val printStream = PrintStream(outputStream)
        printStream.print(rawSearch(request).toString())
        printStream.close()
    }


    open fun count(query: QueryBuilder = QueryBuilders.matchAllQuery()): Long {
        val request = CountRequest(indexName).source(SearchSourceBuilder.searchSource().query(query))
        return kerch.esClient.count(request, RequestOptions.DEFAULT).count
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
              afterIndex: ((Collection<Pair<String, String>>) -> Unit)? = null): IndexBatch {
        return IndexBatch(this, size, waitRefresh, afterIndex)
    }

    fun indexRaw(jsonDocuments: Collection<String>, waitRefresh: Boolean = false) {
        index(jsonDocuments, { null }, { it }, waitRefresh)
    }

    fun indexRaw(jsonDocuments: Map<String, String>, waitRefresh: Boolean = false) {
        index(jsonDocuments.entries, { it.key }, { it.value }, waitRefresh)
    }

    fun index(documents: Collection<ElasticsearchDocument>, waitRefresh: Boolean = false) {
        index(documents, { it.id }, { kerch.toJson(it) }, waitRefresh)
    }

    fun updateField(documentId: String, field: String, value: Any?, waitRefresh: Boolean = false) {
        val doc = jsonBuilder().startObject().field(field, value).endObject()
        val request = UpdateRequest(indexName, KerchConst.DEFAULTTYPE, documentId).doc(doc)
        if (waitRefresh)
            request.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE
        
        kerch.esClient.update(request, RequestOptions.DEFAULT)
    }

    fun painlessUpdate(documentId: String, script: String, params: Map<String, Any?>, waitRefresh: Boolean = false, retryOnConflict: Int = 0) {
        val request = UpdateRequest(indexName, KerchConst.DEFAULTTYPE, documentId)
                .script(Script(ScriptType.INLINE, "painless", script, params))
                .retryOnConflict(retryOnConflict)
        if (waitRefresh)
            request.refreshPolicy = WriteRequest.RefreshPolicy.IMMEDIATE

        kerch.esClient.update(request, RequestOptions.DEFAULT)
    }

    @Throws(ActionRequestValidationException::class)
    fun indexRaw(id: String, jsonString: String, seqNo: Long = 0, waitRefresh: Boolean = false): String {
        var request = indexRequest(id, waitRefresh).type(KerchConst.DEFAULTTYPE)
        if (seqNo > 0) {
            request.setIfSeqNo(seqNo)
        }
        request.source(jsonString, XContentType.JSON)
        val response = kerch.esClient.index(request, RequestOptions.DEFAULT)
        return response.id
    }

    @Throws(ActionRequestValidationException::class)
    fun index(document: ElasticsearchDocument, waitRefresh: Boolean = false): String {
        return indexRaw(document.id, kerch.toJson(document), document.seqNo, waitRefresh)
    }

    @Throws(IndexError::class)
    private fun <T : Any> index(documents: Collection<T>, idProvider: (T) -> String?, sourceProvider: (T) -> String, waitRefresh: Boolean = false) {
        val bulkRequest = BulkRequest()

        if (waitRefresh) {
            bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL
        }

        for (doc in documents) {
            var indexRequest = IndexRequest(indexName, KerchConst.DEFAULTTYPE)

            val id = idProvider(doc)
            if (id != null) {
                indexRequest.id(id)
            }

            indexRequest = indexRequest.source(sourceProvider(doc), XContentType.JSON)
            bulkRequest.add(indexRequest)
        }
        val response = kerch.esClient.bulk(bulkRequest, RequestOptions.DEFAULT)
        if (response.hasFailures()) {
            throw IndexError(response)
        }
    }

    fun createIndex(shards: Int = 5) {
        if (!indexExists) {
            val request = CreateIndexRequest(indexName).settings(Settings.builder().put(INDEX_SHARDS, shards))
            val response = kerch.esClient
                    .indices()
                    .create(request, RequestOptions.DEFAULT)
            kerch.checkResponse(response)
            val refresh = kerch.esClient
                    .indices()
                    .refresh(RefreshRequest(indexName), RequestOptions.DEFAULT)
            if (refresh.successfulShards < 1) {
                //TODO: Better handling
                throw RuntimeException("Index fail in ${refresh.failedShards} shards.")
            }
            log.info("Created index: {}", indexName)
        } else {
            log.info("Index {} already exists. Skipping", indexName)
        }
    }


    val indexExists: Boolean
        get() = kerch.esClient
                .indices()
                .exists(GetIndexRequest(indexName), RequestOptions.DEFAULT)

    fun deleteIndex() {
        val response = kerch.esClient
                .indices()
                .delete(DeleteIndexRequest(indexName), RequestOptions.DEFAULT)
        kerch.checkResponse(response)
    }

    var readOnly: Boolean
        set(value) {
            val request = UpdateSettingsRequest(indexName).settings(Settings.builder().put(INDEX_READONLY, value).build())
            val response = kerch.esClient
                    .indices()
                    .putSettings(request, RequestOptions.DEFAULT)
            kerch.checkResponse(response)
        }
        get() {
            val request = GetSettingsRequest().indices(indexName).names(INDEX_READONLY)
            val value = kerch.esClient
                    .indices()
                    .getSettings(request, RequestOptions.DEFAULT).getSetting(indexName, INDEX_READONLY)
            return value?.toBoolean() ?: false
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

    protected fun doScroll(request: SearchRequest, keepAlive: TimeValue = TimeValue.timeValueMinutes(10)): Sequence<SearchHit> {
        val pageSize = request.source().size()

        var page = kerch.esClient.search(request.scroll(keepAlive), RequestOptions.DEFAULT)
        val totalHits = page.hits.totalHits
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

            hit
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(IndexStore::class.java)

        private const val INDEX_READONLY = "index.blocks.read_only"
        private const val INDEX_SHARDS = "index.number_of_shards"
    }
}