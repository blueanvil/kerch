package com.blueanvil.kerch.search

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.count
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchAction
import org.elasticsearch.action.search.SearchRequestBuilder
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class Search(private val kerch: Kerch,
             private val index: String) {

    fun request(): SearchRequestBuilder {
        return KerchRequestBuilder(kerch, kerch.esClient, SearchAction.INSTANCE)
                .setIndices(index)
                .setTypes(kerch.defaultType)
                .setVersion(true)
    }

    fun docCount(): Long {
        return request().count()
    }
    fun <T : Document> get(id: String, documentType: KClass<T>): T? {
        val get = get(id)
        if (!get.isExists) {
            return null
        }
        return kerch.document(get.sourceAsString, get.version, documentType)
    }

    fun get(id: String, fetchSource: Boolean = true): GetResponse {
        return kerch.esClient
                .prepareGet(index, kerch.defaultType, id)
                .setFetchSource(fetchSource)
                .execute()
                .actionGet()
    }

}