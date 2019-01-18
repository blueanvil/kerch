package com.blueanvil.kerch.index

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.batch.DocumentBatch
import com.blueanvil.kerch.batch.IndexBatch
import com.blueanvil.kerch.error.IndexError
import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.engine.VersionConflictEngineException

/**
 * @author Cosmin Marginean
 */
class Indexer(private val kerch: Kerch,
              private val index: String) {


    fun batch(size: Int = 100, afterIndex: ((Collection<String>) -> Unit)? = null): IndexBatch {
        return IndexBatch(this, size, afterIndex)
    }

    fun <T : Document> batch(size: Int = 100, afterIndex: ((Collection<T>) -> Unit)? = null): DocumentBatch<T> {
        return DocumentBatch(this, size, afterIndex)
    }

    fun indexRaw(jsonDocument: Collection<String>) {
        index(jsonDocument, { null }, { it })
    }

    fun index(documents: Collection<Document>) {
        index(documents, { it.id }, { toJsonString(it) })
    }

    @Throws(VersionConflictEngineException::class)
    fun index(document: Document, waitRefresh: Boolean = false): String {
        var request = indexRequest(document.id, waitRefresh)
        if (document.version > 0) {
            request.setVersion(document.version)
        }
        request = request.setSource(toJsonString(document), XContentType.JSON)
        val response = request.execute().actionGet()
        return response.id
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

    private fun toJsonString(document: Document) = kerch.objectMapper.writeValueAsString(document)

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
        return kerch.esClient.prepareIndex(index, kerch.defaultType)
    }

    fun delete(id: String, waitRefresh: Boolean = false) {
        var request = kerch.esClient.prepareDelete(index, Kerch.TYPE, id)
        if (waitRefresh) {
            request = request.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL)
        }
        request.execute().actionGet()
    }
}