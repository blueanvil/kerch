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
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.client.core.CountRequest
import org.elasticsearch.client.indices.CreateIndexRequest
import org.elasticsearch.client.indices.GetIndexRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.fetch.subphase.FetchSourceContext
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass


/**
 * @author Cosmin Marginean
 */
open class IndexStore(protected val kerch: Kerch,
                      private val index: String,
                      private val indexMapper: (String) -> String = { it }) {

    val indexName: String get() = indexMapper(index)

    fun <T : ElasticsearchDocument> get(id: String, documentType: KClass<T>): T? {
        val response = get(id)
        if (!response.isExists) {
            return null
        }
        return kerch.document(response.sourceAsString, response.seqNo, documentType)
    }

    fun <T : ElasticsearchDocument> typed(docType: KClass<T>): TypedIndexStore<T> {
        return TypedIndexStore(kerch, index, docType)
    }

    fun get(id: String, fetchSource: Boolean = true): GetResponse {
        val request = GetRequest(indexName, id)
                .fetchSourceContext(if (fetchSource) FetchSourceContext.FETCH_SOURCE else FetchSourceContext.DO_NOT_FETCH_SOURCE)
        return kerch.esClient.get(request, RequestOptions.DEFAULT)
    }


    fun search(query: QueryBuilder = QueryBuilders.matchAllQuery()): KerchSearchRequest {
        return KerchSearchRequest(kerch, SearchRequest(indexName).source(SearchSourceBuilder().query(query)))
    }

    fun count(query: QueryBuilder = QueryBuilders.matchAllQuery()): Long {
        return kerch.esClient.count(CountRequest(indexName).query(query), RequestOptions.DEFAULT).count
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
    fun index(document: ElasticsearchDocument, waitRefresh: Boolean = false): String {
        return indexRaw(document.id, kerch.toJson(document), document.seqNo, waitRefresh)
    }

    fun delete(id: String, waitRefresh: Boolean = false) {
        val request = DeleteRequest(indexName).id(id)
        if (waitRefresh) {
            request.refreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL
        }
        kerch.esClient.delete(request, RequestOptions.DEFAULT)
    }

    @Throws(IndexError::class)
    private fun <T : Any> index(documents: Collection<T>, idProvider: (T) -> String?, sourceProvider: (T) -> String, waitRefresh: Boolean = false) {
        val bulkRequest = BulkRequest()

        if (waitRefresh) {
            bulkRequest.refreshPolicy = WriteRequest.RefreshPolicy.WAIT_UNTIL
        }

        for (doc in documents) {
            var indexRequest = IndexRequest(indexName)

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
            request = request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
        }
        return request
    }

    companion object {
        private val log = LoggerFactory.getLogger(IndexStore::class.java)

        private const val INDEX_READONLY = "index.blocks.read_only"
        private const val INDEX_SHARDS = "index.number_of_shards"
    }
}