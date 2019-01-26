package com.blueanvil.kerch.search

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.allHits
import org.elasticsearch.action.search.SearchAction
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.ElasticsearchClient

/**
 * @author Cosmin Marginean
 */
class KerchRequest<T : Document?>(internal val kerch: Kerch,
                                 private val toDocument: (String) -> T,
                                 client: ElasticsearchClient,
                                 action: SearchAction) : SearchRequestBuilder(client, action) {

    fun all(): Sequence<T> {
        return this.allHits().map { toDocument(it.sourceAsString) }
    }

}
