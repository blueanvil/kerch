package com.blueanvil.kerch

import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.elasticsearch.search.sort.SortBuilder
import org.elasticsearch.search.sort.SortOrder

/**
 * @author Cosmin Marginean
 */
fun SearchRequest.paging(from: Int, size: Int): SearchRequest {
    updateSource()
    source().from(from).size(size)
    return this
}

fun SearchRequest.query(query: QueryBuilder): SearchRequest {
    updateSource()
    source().query(query)
    return this
}

fun SearchRequest.size(size: Int): SearchRequest {
    updateSource()
    source().size(size)
    return this
}

fun SearchRequest.sort(field: String, order: SortOrder = SortOrder.ASC): SearchRequest {
    updateSource()
    source().sort(field, order)
    return this
}

fun SearchRequest.sort(sort: SortBuilder<*>): SearchRequest {
    updateSource()
    source().sort(sort)
    return this
}

private fun SearchRequest.updateSource() {
    if (source() == null) {
        source(SearchSourceBuilder.searchSource())
    }
}


