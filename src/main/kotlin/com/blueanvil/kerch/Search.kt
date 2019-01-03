package com.blueanvil.kerch

import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchAction
import org.elasticsearch.action.search.SearchRequestBuilder

/**
 * @author Cosmin Marginean
 */
class Search(private val kerch: Kerch,
             private val indexProvider: () -> String) {

    fun request(): SearchRequestBuilder {
        return KerchRequestBuilder(kerch, kerch.esClient, SearchAction.INSTANCE)
                .setIndices(indexProvider())
                .setTypes(kerch.defaultType)
                .setVersion(true)
    }


    fun get(id: String, fetchSource: Boolean = true): GetResponse {
        return kerch.esClient
                .prepareGet(indexProvider(), kerch.defaultType, id)
                .setFetchSource(fetchSource)
                .execute()
                .actionGet()
    }
}