package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.TypedIndexStore
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
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

    fun find(query: QueryBuilder): Sequence<T> {
        return search(query).scroll().map { kerch.document(it, docType) }
    }
}