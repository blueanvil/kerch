package com.blueanvil.kerch

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
            internal val toDocument: (String, KClass<out ElasticsearchDocument>) -> ElasticsearchDocument,
            internal val toJson: (ElasticsearchDocument) -> String,

        //TODO: This is a temporary requirement until ES removes types altogether: https://www.elastic.co/guide/en/elasticsearch/reference/6.x/removal-of-types.html
            internal val defaultType: String = TYPE) {

    constructor(clusterName: String,
                nodes: Collection<String>,
                toDocument: (String, KClass<out ElasticsearchDocument>) -> ElasticsearchDocument,
                toJson: (ElasticsearchDocument) -> String,
                defaultType: String = TYPE) :
            this(esClient = transportClient(clusterName, nodes),
                    toDocument = toDocument,
                    toJson = toJson,
                    defaultType = defaultType)

    constructor(clusterName: String,
                nodes: Collection<String>,
                objectMapper: ObjectMapper = jacksonObjectMapper(),
                defaultType: String = TYPE) : this(esClient = transportClient(clusterName, nodes),
            toDocument = { json: String, docType: KClass<out ElasticsearchDocument> -> Kerch.toDocument(objectMapper, json, docType) },
            toJson = { document -> Kerch.toJson(objectMapper, document) },
            defaultType = defaultType) {
        objectMapper.findAndRegisterModules()
    }

    val admin = Admin(this)

    fun store(index: String): IndexStore {
        return IndexStore(this, index)
    }

    fun <T : ElasticsearchDocument> typedStore(index: String, docType: KClass<T>): TypedIndexStore<T> {
        return TypedIndexStore(this, index, docType)
    }

    fun <T : ElasticsearchDocument> document(hit: SearchHit, documentType: KClass<T>): T {
        return document(hit.sourceAsString, hit.version, documentType)
    }

    fun <T : ElasticsearchDocument> document(sourceAsString: String, version: Long, documentType: KClass<T>): T {
        val document = toDocument(sourceAsString, documentType)
        document.version = version
        return document as T
    }

    fun indexWrapper(alias: String): IndexWrapper {
        return IndexWrapper(this, alias)
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

        internal fun transportClient(clusterName: String, nodes: Collection<String>): Client {
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

        private fun toJson(objectMapper: ObjectMapper, document: ElasticsearchDocument): String = objectMapper.writeValueAsString(document)

        private fun <T : ElasticsearchDocument> toDocument(objectMapper: ObjectMapper, jsonString: String, documentType: KClass<T>): T = objectMapper.readValue(jsonString, documentType.javaObjectType)
    }
}

