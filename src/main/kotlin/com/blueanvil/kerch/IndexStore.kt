package com.blueanvil.kerch

import com.blueanvil.kerch.batch.IndexBatch
import com.blueanvil.kerch.error.IndexError
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.engine.VersionConflictEngineException
import org.slf4j.LoggerFactory
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
open class IndexStore(protected val kerch: Kerch,
                      private val index: String,
                      private val indexMapper: (String) -> String = { it }) {

    val indexName: String get() = indexMapper(index)

    fun <T : Document> get(id: String, documentType: KClass<T>): T? {
        val response = get(id)
        if (!response.isExists) {
            return null
        }
        return kerch.document(response.sourceAsString, response.version, documentType)
    }

    fun <T : Document> typed(docType: KClass<T>): TypedIndexStore<T> {
        return TypedIndexStore(kerch, index, docType)
    }

    fun get(id: String, fetchSource: Boolean = true): GetResponse {
        return kerch.esClient
                .prepareGet(indexName, kerch.defaultType, id)
                .setFetchSource(fetchSource)
                .execute()
                .actionGet()
    }


    fun search(): SearchRequestBuilder {
        return kerch.esClient.prepareSearch()
                .setIndices(indexName)
                .setTypes(kerch.defaultType)
                .setVersion(true)
    }

    fun batch(size: Int = 100,
              afterIndex: ((Collection<Pair<String, String>>) -> Unit)? = null): IndexBatch {
        return IndexBatch(this, size, afterIndex)
    }

    fun indexRaw(jsonDocuments: Collection<String>) {
        index(jsonDocuments, { null }, { it })
    }

    fun indexRaw(jsonDocuments: Map<String, String>) {
        index(jsonDocuments.entries, { it.key }, { it.value })
    }

    fun index(documents: Collection<Document>) {
        index(documents, { it.id }, { kerch.toJson(it) })
    }

    @Throws(VersionConflictEngineException::class)
    fun indexRaw(id: String, jsonString: String, version: Long = 0, waitRefresh: Boolean = false): String {
        var request = indexRequest(id, waitRefresh)
        if (version > 0) {
            request.setVersion(version)
        }
        request = request.setSource(jsonString, XContentType.JSON)
        val response = request.execute().actionGet()
        return response.id
    }

    @Throws(VersionConflictEngineException::class)
    fun index(document: Document, waitRefresh: Boolean = false): String {
        return indexRaw(document.id, kerch.toJson(document), document.version)
    }

    fun delete(id: String, waitRefresh: Boolean = false) {
        var request = kerch.esClient.prepareDelete(indexName, Kerch.TYPE, id)
        if (waitRefresh) {
            request = request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL)
        }
        request.execute().actionGet()
    }

    @Throws(IndexError::class)
    private fun <T : Any> index(documents: Collection<T>, idProvider: (T) -> String?, sourceProvider: (T) -> String) {
        val request = kerch.esClient.prepareBulk()
        for (doc in documents) {
            var indexRequest = prepareIndex()

            val id = idProvider(doc)
            if (id != null) {
                indexRequest.setId(id)
            }

            indexRequest = indexRequest.setSource(sourceProvider(doc), XContentType.JSON)
            request.add(indexRequest)
        }
        val response = request.execute().actionGet()
        if (response.hasFailures()) {
            throw IndexError(response)
        }
    }

    fun createIndex(shards: Int = 5) {
        if (!indexExists) {
            var request = CreateIndexRequest(indexName)

            val settings = Settings.builder().put("index.number_of_shards", shards)
            request = request.settings(settings)

            val response = kerch.esClient
                    .admin()
                    .indices()
                    .create(request)
                    .actionGet()
            kerch.checkResponse(response)
            val refresh = kerch.esClient
                    .admin()
                    .indices()
                    .prepareRefresh(indexName)
                    .execute()
                    .actionGet()
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
        get() {
            val response = kerch.esClient
                    .admin()
                    .indices()
                    .prepareExists(indexName)
                    .execute()
                    .actionGet()
            return response.isExists
        }

    fun deleteIndex() {
        val deleteRequest = DeleteIndexRequest(indexName)
        val response = kerch.esClient
                .admin()
                .indices()
                .delete(deleteRequest)
                .actionGet()
        kerch.checkResponse(response)
    }

    var readOnly: Boolean
        set(value) {
            val response = kerch.esClient
                    .admin()
                    .indices()
                    .prepareUpdateSettings(indexName)
                    .setSettings(Settings.builder().put(INDEX_READONLY, value))
                    .execute()
                    .actionGet()
            kerch.checkResponse(response)
        }
        get () {
            val setting = kerch.esClient
                    .admin()
                    .indices()
                    .prepareGetSettings(indexName)
                    .execute()
                    .actionGet()
                    .getSetting(indexName, INDEX_READONLY)
            return setting?.toBoolean() ?: false
        }

    private fun indexRequest(id: String?, waitRefresh: Boolean = false): IndexRequestBuilder {
        var request = prepareIndex()
        if (id != null) {
            request = request.setId(id)
        }
        if (waitRefresh) {
            request = request.setRefreshPolicy(WriteRequest.RefreshPolicy.IMMEDIATE)
        }
        return request
    }

    private fun prepareIndex(): IndexRequestBuilder {
        return kerch.esClient.prepareIndex(indexName, kerch.defaultType)
    }

    companion object {
        private val log = LoggerFactory.getLogger(IndexStore::class.java)

        private const val INDEX_READONLY = "index.blocks.read_only"
    }
}