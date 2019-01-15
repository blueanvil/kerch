package com.blueanvil.kerch

import com.blueanvil.kerch.search.KerchRequestBuilder
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.search.SearchHit

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

fun SearchRequestBuilder.scroll(perPage: Int = 100, keepAlive: TimeValue = TimeValue.timeValueMinutes(10)): Sequence<SearchHit> {
    //TODO: This isn't very elegant
    if (this !is KerchRequestBuilder) {
        throw IllegalStateException("Current request is not a KerchRequestBuilder")
    }

    this.setScroll(keepAlive)
    this.setSize(perPage)

    var page = this.execute().actionGet()
    val totalHits = page.hits.totalHits
    var crtIndex = 0
    var scrollId = page.scrollId

    return generateSequence {
        var hit: SearchHit? = null
        if (totalHits > 0 && crtIndex < totalHits) {
            var crtPageIndex = crtIndex % perPage
            hit = page.hits.hits[crtPageIndex]
            crtIndex++
            if (crtIndex % perPage == 0) {
                page = this.kerch.esClient.prepareSearchScroll(scrollId)
                        .setScroll(keepAlive)
                        .execute()
                        .actionGet()
                scrollId = page.scrollId
            }
        }

        hit
    }
}


fun SearchRequestBuilder.count(): Long {
    setSize(0)
    return execute().actionGet().hits.totalHits
}
