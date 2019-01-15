package com.blueanvil.kerch.search

import com.blueanvil.kerch.Kerch
import org.elasticsearch.action.search.SearchAction
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.ElasticsearchClient

/**
 * @author Cosmin Marginean
 */
class KerchRequestBuilder(internal val kerch: Kerch,
                          client: ElasticsearchClient,
                          action: SearchAction)
    : SearchRequestBuilder(client, action)
