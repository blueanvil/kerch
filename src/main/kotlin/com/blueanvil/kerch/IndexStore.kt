package com.blueanvil.kerch

import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.search.SearchHit
import kotlin.reflect.KClass


/**
 * @author Cosmin Marginean
 */
class IndexStore(kerch: Kerch,
                 index: String,
                 indexMapper: (String) -> String) : IndexStoreBase<SearchHit>(kerch, index, indexMapper) {

    fun <T : ElasticsearchDocument> get(id: String, documentType: KClass<T>): T? {
        val response = doGet(id)
        if (!response.isExists) {
            return null
        }
        return kerch.document(response.sourceAsString, response.seqNo, documentType)
    }

    override fun search(request: SearchRequest): List<SearchHit> {
        return kerch.esClient.search(request, RequestOptions.DEFAULT).hits.hits.toList()
    }

    override fun scroll(request: SearchRequest): Sequence<SearchHit> {
        return doScroll(request)
    }
}