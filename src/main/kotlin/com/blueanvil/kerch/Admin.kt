package com.blueanvil.kerch

import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateAction
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest
import org.elasticsearch.action.support.master.AcknowledgedResponse
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author Cosmin Marginean
 */
class Admin(private val kerch: Kerch) {

    fun createTemplate(templateName: String, jsonContent: String) {
        val request = PutIndexTemplateRequest(templateName).source(jsonContent, XContentType.JSON)
        val response = kerch.esClient.admin()
                .indices()
                .execute(PutIndexTemplateAction.INSTANCE, request)
                .actionGet()
        checkResponse(response)
        log.info("Created template {}", templateName)
    }

    fun createIndex(indexName: String, shards: Int = 5) {
        val index: String = kerch.indexMapper(indexName)
        if (!indexExists(index)) {
            var request = CreateIndexRequest(index)

            val settings = Settings.builder().put("index.number_of_shards", shards)
            request = request.settings(settings)

            val response = kerch.esClient
                    .admin()
                    .indices()
                    .create(request)
                    .actionGet()
            checkResponse(response)
            val refresh = kerch.esClient
                    .admin()
                    .indices()
                    .prepareRefresh(index)
                    .execute()
                    .actionGet()
            if (refresh.successfulShards < 1) {
                //TODO: Better handling
                throw RuntimeException("Index fail in ${refresh.failedShards} shards.")
            }
            log.info("Created index: {}", index)
        } else {
            log.info("Index {} already exists. Skipping", index)
        }
    }

    fun indexExists(index: String): Boolean {
        val response = kerch.esClient
                .admin()
                .indices()
                .prepareExists(kerch.indexMapper(index))
                .execute()
                .actionGet()
        return response.isExists
    }

    fun aliasExists(aliasName: String): Boolean {
        val response = kerch.esClient
                .admin()
                .indices()
                .prepareAliasesExist(kerch.indexMapper(aliasName))
                .execute()
                .actionGet()
        return response.exists()
    }

    fun createAlias(aliasName: String, vararg indices: String) {
        val response = kerch.esClient
                .admin()
                .indices()
                .prepareAliases()
                .addAlias(indices, aliasName)
                .execute()
                .actionGet()
        checkResponse(response)
    }

    fun indicesForAlias(alias: String): List<String> {
        val response = kerch.esClient
                .admin()
                .indices()
                .getAliases(GetAliasesRequest(alias))
                .actionGet()
        val indices = ArrayList<String>()
        response.aliases
                .forEach { cursor ->
                    if (cursor.key != null && cursor.value != null && cursor.value.size > 0) {
                        indices.add(cursor.key)
                    }
                }
        return indices
    }

    private fun checkResponse(response: AcknowledgedResponse) {
        if (!response.isAcknowledged) {
            //TODO: Better handling
            throw RuntimeException("Error executing Elasticsearch request")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(Admin::class.java)
    }
}