package com.blueanvil.kerch

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.apache.http.HttpHost
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.client.RestClient
import org.elasticsearch.client.RestHighLevelClient
import org.elasticsearch.index.query.QueryBuilder
import org.elasticsearch.search.SearchHit
import org.slf4j.LoggerFactory
import java.net.InetAddress
import kotlin.reflect.KClass


/**
 * @author Cosmin Marginean
 */
class Kerch(internal val esClient: RestHighLevelClient,
            internal val toDocument: (String, KClass<out ElasticsearchDocument>) -> ElasticsearchDocument,
            internal val toJson: (ElasticsearchDocument) -> String) {

    constructor(nodes: Collection<String>,
                toDocument: (String, KClass<out ElasticsearchDocument>) -> ElasticsearchDocument,
                toJson: (ElasticsearchDocument) -> String) :
            this(esClient = restClient(nodes),
                    toDocument = toDocument,
                    toJson = toJson)

    constructor(nodes: Collection<String>,
                objectMapper: ObjectMapper = jacksonObjectMapper()) : this(esClient = restClient(nodes),
            toDocument = { json: String, docType: KClass<out ElasticsearchDocument> -> toDocument(objectMapper, json, docType) },
            toJson = { document -> toJson(objectMapper, document) })

    val admin = Admin(this)

    fun store(index: String, indexMapper: (String) -> String = { it }): IndexStore {
        return IndexStore(this, index, indexMapper)
    }

    fun <T : ElasticsearchDocument> typedStore(index: String,
                                               docType: KClass<T>,
                                               indexMapper: (String) -> String = { it },
                                               adaptQuery: (QueryBuilder) -> QueryBuilder = { it }): TypedIndexStore<T> {
        return TypedIndexStore(this, index, docType, indexMapper, adaptQuery)
    }

    fun <T : ElasticsearchDocument> document(hit: SearchHit, documentType: KClass<T>): T {
        return document(hit.sourceAsString, hit.seqNo, documentType)
    }

    fun <T : ElasticsearchDocument> document(sourceAsString: String, seqNo: Long, documentType: KClass<T>): T {
        val document = toDocument(sourceAsString, documentType)
        document.seqNo = seqNo
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

        internal fun restClient(nodes: Collection<String>): RestHighLevelClient {
            log.info("ElasticSearch connection: nodes=${nodes.joinToString(",")}")
            val hosts = nodes.map { nodeAddr ->
                val addressElements = nodeAddr.trim().split(':')
                if (addressElements.size != 2) {
                    throw IllegalArgumentException(String.format("Address %s has incorrect format (hostname:port)", nodeAddr))
                }
                val hostname = addressElements[0].trim()
                val port = addressElements[1].trim().toInt()
                val byName = InetAddress.getByName(hostname)
                HttpHost(byName, port, "http")
            }.toTypedArray()

            return RestHighLevelClient(RestClient.builder(*hosts))
        }

        private fun toJson(objectMapper: ObjectMapper, document: ElasticsearchDocument): String = objectMapper.writeValueAsString(document)

        private fun <T : ElasticsearchDocument> toDocument(objectMapper: ObjectMapper, jsonString: String, documentType: KClass<T>): T = objectMapper.readValue(jsonString, documentType.javaObjectType)
    }
}

