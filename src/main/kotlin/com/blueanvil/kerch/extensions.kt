package com.blueanvil.kerch

import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.SearchHit
import java.io.OutputStream
import java.io.PrintStream
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

/**
 * @author Cosmin Marginean
 */

private val clientProp: KProperty1<SearchRequestBuilder, *> = clientProp()

fun clientProp(): KProperty1<SearchRequestBuilder, *> {
    val prop = SearchRequestBuilder::class.memberProperties.find { it.name == "client" }!!
    prop.isAccessible = true
    return prop
}

fun SearchRequestBuilder.paging(from: Int = 0, count: Int = 10): SearchRequestBuilder {
    return this.setFrom(from)
            .setSize(count)
}

fun SearchRequestBuilder.docCount(): Long {
    return setSize(0)
            .execute()
            .actionGet()
            .hits
            .totalHits
}

fun SearchRequestBuilder.allIds(): Sequence<String> {
    return setQuery(QueryBuilders.matchAllQuery())
            .setFetchSource(false)
            .allHits()
            .map { hit -> hit.id }
}

fun SearchRequestBuilder.ids(): Sequence<String> {
    return setFetchSource(false)
            .execute()
            .actionGet()
            .hits
            .asSequence()
            .map { hit -> hit.id }
}

fun SearchRequestBuilder.write(outputStream: OutputStream) {
    val response = execute().actionGet()
    val printStream = PrintStream(outputStream)
    printStream.print(response.toString())
    printStream.flush()
}

fun SearchRequestBuilder.hits(): Sequence<SearchHit> {
    return this.execute().actionGet()
            .hits
            .asSequence()
}

fun SearchRequestBuilder.allHits(perPage: Int = 10,
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
    this.setScroll(keepAlive)
    this.setSize(perPage)

    var page = this.execute().actionGet()
    val totalHits = page.hits.totalHits
    var crtIndex = 0
    var scrollId = page.scrollId
    val client = clientProp.get(this) as Client

    return generateSequence {
        var hit: SearchHit? = null
        if (totalHits > 0 && crtIndex < totalHits) {
            var crtPageIndex = crtIndex % perPage
            hit = page.hits.hits[crtPageIndex]
            crtIndex++
            if (crtIndex % perPage == 0) {
                page = client.prepareSearchScroll(scrollId)
                        .setScroll(keepAlive)
                        .execute()
                        .actionGet()
                scrollId = page.scrollId
            }
        }

        hit
    }
}

