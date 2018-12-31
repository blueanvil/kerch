package com.blueanvil.kerch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.SearchHit
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */

fun SearchRequestBuilder.paging(from: Int = 0, count: Int = 10): SearchRequestBuilder {
    return this.setFrom(from)
            .setSize(count)
}

fun SearchRequestBuilder.hits(perPage: Int = 10,
                              maxResults: Int = 10_000): Sequence<SearchHit> {
    this.setSize(perPage)
    var page = this.execute().actionGet()
    val totalHits = page.hits.totalHits
    var crtIndex = 0

    return generateSequence {
        var hit: SearchHit? = null
        if (totalHits > 0 && crtIndex < totalHits && crtIndex < maxResults) {
            var crtPageIndex = crtIndex % perPage
            hit = page.hits.hits[crtPageIndex]
            crtIndex++
            if (crtIndex % perPage == 0 && crtIndex < maxResults) {
                page = this.setFrom(crtIndex).execute().actionGet()
            }
        }

        hit
    }
}


fun <T : Document> SearchRequestBuilder.documents(documentType: KClass<T>,
                                                  perPage: Int = 10,
                                                  maxResults: Int = 10_000): Sequence<T> {
    if (this !is KerchRequestBuilder) {
        throw IllegalStateException("Current request is not a KerchRequestBuilder")
    }

    val objectMapper = this.kerch.objectMapper
    return hits(perPage, maxResults).map { hit ->
        document(hit.sourceAsString, hit.version, documentType, objectMapper)
    }
}

fun SearchRequestBuilder.count(): Long {
    setSize(0)
    return execute().actionGet().hits.totalHits
}

fun <T : Document> GetResponse.toDocument(documentType: KClass<T>,
                                          objectMapper: ObjectMapper = jacksonObjectMapper()): T? {
    return if (isExists) {
        document(sourceAsString, version, documentType, objectMapper)
    } else {
        null
    }
}


private fun <T : Document> document(sourceAsString: String, version: Long, documentType: KClass<T>, objectMapper: ObjectMapper): T {
    val document = objectMapper.readValue(sourceAsString, documentType.javaObjectType)
    document.version = version
    return document
}