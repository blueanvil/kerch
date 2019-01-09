package com.blueanvil.kerch

import org.elasticsearch.action.index.IndexRequestBuilder
import org.elasticsearch.action.support.WriteRequest
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.engine.VersionConflictEngineException

/**
 * @author Cosmin Marginean
 */
class Indexer(private val kerch: Kerch,
              private val index: String) {

    fun <T : Document> batch(size: Int = 10): IndexBatch<T> {
        return IndexBatch(this, size)
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

    fun index(documents: Collection<Document>) {
        val request = kerch.esClient.prepareBulk()
        for (document in documents) {
            var indexRequest = prepareIndex()

            if (document.id != null) {
                indexRequest.setId(document.id)
            }

            val source = toJsonString(document)
            indexRequest = indexRequest.setSource(source, XContentType.JSON)
            request.add(indexRequest)
        }
        val response = request.execute().actionGet()
        if (response.hasFailures()) {
            //TODO Better handling
            throw RuntimeException("Could not index all documents. Error message is: " + response.buildFailureMessage())
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
}