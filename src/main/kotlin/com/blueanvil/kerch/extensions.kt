package com.blueanvil.kerch

import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder

/**
 * @author Cosmin Marginean
 */
fun SearchRequest.paging(from: Int, size: Int): SearchRequest {
    if (source() == null) {
        source(SearchSourceBuilder.searchSource())
    }
    source().from(from).size(size)
    return this
}

fun SearchRequest.query(query: QueryBuilder): SearchRequest {
    if (source() == null) {
        source(SearchSourceBuilder.searchSource())
    }
    source().query(query)
    return this
}

fun SearchRequest.size(size: Int): SearchRequest {
    if (source() == null) {
        source(SearchSourceBuilder.searchSource())
    }
    source().size(size)
    return this
}
