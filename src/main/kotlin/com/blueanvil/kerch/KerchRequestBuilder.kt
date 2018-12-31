package com.blueanvil.kerch

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