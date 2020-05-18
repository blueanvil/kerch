package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.*
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class NestieIndexStore<T : ElasticsearchDocument>(kerch: Kerch,
                                                  index: String,
                                                  docType: KClass<T>,
                                                  indexMapper: (String) -> String = { it }) : TypedIndexStore<T>(kerch, index, docType, indexMapper) {

    private val annotation: NestieDoc = Nestie.annotation(docType)

    fun field(fieldName: String): String {
        return "${annotation.type}.$fieldName"
    }

    fun findAll(): Sequence<T> {
        return find(QueryBuilders.matchAllQuery())
    }

    fun list(query: QueryBuilder, from: Int, size: Int, sortField: String = Const.FIELD_ID, sortOrder: SortOrder = SortOrder.ASC): List<T> {
        return search()
                .setQuery(query)
                .setFrom(from)
                .setSize(size)
                .addSort(sortField, sortOrder)
                .hits()
                .map { kerch.document(it, docType) }
                .toList()
    }

    fun find(query: QueryBuilder): Sequence<T> {
        return search()
                .setQuery(query)
                .allHits()
                .map { kerch.document(it, docType) }
    }
}