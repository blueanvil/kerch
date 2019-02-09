package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.TypedIndexStore
import com.blueanvil.kerch.allHits
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.index.query.QueryBuilders
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class NestieIndexStore<T : ElasticsearchDocument>(kerch: Kerch,
                                                  index: String,
                                                  docType: KClass<T>) : TypedIndexStore<T>(kerch, index, docType) {

    private val annotation: NestieDoc = Nestie.annotation(docType)

    fun field(fieldName: String): String {
        return "${annotation.type}.$fieldName"
    }

    fun findAll(): Sequence<T> {
        return find(QueryBuilders.existsQuery(field("id")))
    }

    fun find(query: QueryBuilder): Sequence<T> {
        return search()
                .setQuery(query)
                .allHits()
                .map { kerch.document(it, docType) }
    }
}