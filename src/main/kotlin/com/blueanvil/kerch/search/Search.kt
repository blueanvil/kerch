package com.blueanvil.kerch.search

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.count
import com.blueanvil.kerch.scroll
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchAction
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
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

    fun allIds(): Sequence<String> {
        return ids(QueryBuilders.matchAllQuery())
    }

    fun ids(query: QueryBuilder): Sequence<String> {
        return request().setQuery(query).scroll().map { hit -> hit.id }
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