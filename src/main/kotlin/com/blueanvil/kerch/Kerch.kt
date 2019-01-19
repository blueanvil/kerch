package com.blueanvil.kerch

import com.blueanvil.kerch.index.Index
import com.blueanvil.kerch.index.IndexWrapper
import com.blueanvil.kerch.index.Indexer
import com.blueanvil.kerch.search.Search
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.search.SearchHit
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.slf4j.LoggerFactory
import java.net.InetAddress
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class Kerch(internal val esClient: Client,
            val objectMapper: ObjectMapper = jacksonObjectMapper(),

        //TODO: This is a temporary requirement until ES removes types altogether: https://www.elastic.co/guide/en/elasticsearch/reference/6.x/removal-of-types.html
            internal val defaultType: String = TYPE) {

    constructor(clusterName: String,
                nodes: Collection<String>,
                objectMapper: ObjectMapper = jacksonObjectMapper(),
                defaultType: String = TYPE) : this(transportClient(clusterName, nodes), objectMapper, defaultType)

    val admin = Admin(this)

    fun indexer(index: String): Indexer {
        return Indexer(this, index)
    }

    fun search(index: String): Search {
        return Search(this, index)
    }

    fun index(index: String): Index {
        return Index(this, index)
    }

    fun indexWrapper(alias: String): IndexWrapper {
        return IndexWrapper(this, alias)
    }

    fun <T : Document> document(hit: SearchHit, documentType: KClass<T>): T {
        return document(hit.sourceAsString, hit.version, documentType)
    }

    fun <T : Document> document(sourceAsString: String, version: Long, documentType: KClass<T>): T {
        val document = objectMapper.readValue(sourceAsString, documentType.javaObjectType)
        document.version = version
        return document
    }

    internal fun checkResponse(response: AcknowledgedResponse) {
        if (!response.isAcknowledged) {
            //TODO: Better handling
            throw RuntimeException("Error executing Elasticsearch request")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Kerch::class.java)
        const val TYPE = "defaulttype"

        private fun transportClient(clusterName: String, nodes: Collection<String>): Client {
            log.info("ElasticSearch connection: cluster=$clusterName, nodes=${nodes.joinToString(",")}")
            val settings = Settings.builder().put("cluster.name", clusterName).build()
            val client = PreBuiltTransportClient(settings)
            for (nodeAddr in nodes) {
                val addressElements = nodeAddr.trim().split(':')
                if (addressElements.size != 2) {
                    throw IllegalArgumentException(String.format("Address %s has incorrect format (hostname:port)", nodeAddr))
                }
                val hostname = addressElements[0].trim()
                val port = addressElements[1].trim().toInt()
                val byName = InetAddress.getByName(hostname)
                client.addTransportAddress(TransportAddress(byName, port))
            }
            return client
        }
    }
}

