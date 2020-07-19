package com.blueanvil.kerch

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.action.admin.indices.get.GetIndexRequest
import org.elasticsearch.action.admin.indices.refresh.RefreshRequest
import org.elasticsearch.action.admin.indices.settings.get.GetSettingsRequest
import org.elasticsearch.action.admin.indices.settings.put.UpdateSettingsRequest
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest
import org.elasticsearch.client.Request
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.util.*


/**
 * @author Cosmin Marginean
 */
class Admin(private val kerch: Kerch) {

    companion object {
        private val log = LoggerFactory.getLogger(Admin::class.java)

        private const val INDEX_READONLY = "index.blocks.read_only"
        private const val INDEX_SHARDS = "index.number_of_shards"
    }

    fun createIndex(index: String, shards: Int = 5) {
        if (!indexExists(index)) {
            val request = CreateIndexRequest(index).settings(Settings.builder().put(INDEX_SHARDS, shards))
            val response = kerch.esClient
                    .indices()
                    .create(request, RequestOptions.DEFAULT)
            kerch.checkResponse(response)
            val refresh = kerch.esClient
                    .indices()
                    .refresh(RefreshRequest(index), RequestOptions.DEFAULT)
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
        return kerch.esClient
                .indices()
                .exists(GetIndexRequest().indices(index), RequestOptions.DEFAULT)
    }

    fun deleteIndex(index: String) {
        val response = kerch.esClient
                .indices()
                .delete(DeleteIndexRequest(index), RequestOptions.DEFAULT)
        kerch.checkResponse(response)
    }

    fun setReadOnly(index: String, readOnly: Boolean) {
        val request = UpdateSettingsRequest(index).settings(Settings.builder().put(INDEX_READONLY, readOnly).build())
        val response = kerch.esClient
                .indices()
                .putSettings(request, RequestOptions.DEFAULT)
        kerch.checkResponse(response)
    }

    fun isReadOnly(index: String): Boolean {
        val request = GetSettingsRequest().indices(index).names(INDEX_READONLY)
        val value = kerch.esClient
                .indices()
                .getSettings(request, RequestOptions.DEFAULT).getSetting(index, INDEX_READONLY)
        return value?.toBoolean() ?: false
    }

    fun createTemplate(templateName: String, jsonContent: String) {
        val request = PutIndexTemplateRequest(templateName).source(jsonContent, XContentType.JSON)
        val response = kerch.esClient.indices().putTemplate(request, RequestOptions.DEFAULT)
        kerch.checkResponse(response)
        log.info("Created template {}", templateName)
    }

    fun aliasExists(alias: String): Boolean {
        return kerch.esClient
                .indices()
                .existsAlias(GetAliasesRequest(alias), RequestOptions.DEFAULT)
    }

    fun createAlias(alias: String, vararg indices: String) {
        val request = IndicesAliasesRequest()
        indices.forEach { index ->
            val aliasAction = AliasActions(AliasActions.Type.ADD)
                    .index(index)
                    .alias(alias)
            request.addAliasAction(aliasAction)
        }

        val response = kerch.esClient
                .indices()
                .updateAliases(request, RequestOptions.DEFAULT)
        kerch.checkResponse(response)
    }

    fun indicesForAlias(alias: String): List<String> {
        val response = kerch.esClient
                .indices()
                .getAlias(GetAliasesRequest(alias), RequestOptions.DEFAULT)
        val indices = ArrayList<String>()
        response.aliases
                .forEach { aliasMapping ->
                    if (aliasMapping.key != null && aliasMapping.value != null && aliasMapping.value.size > 0) {
                        indices.add(aliasMapping.key)
                    }
                }
        return indices
    }

    fun deleteAlias(alias: String, index: String) {
        throw UnsupportedOperationException("This operation is not implemented")
//        kerch.esClient.indices().deleteAlias(DeleteAliasRequest(alias, index), RequestOptions.DEFAULT)
    }

    fun moveAlias(alias: String, fromIndex: String, toIndex: String) {
        deleteAlias(alias, fromIndex)
        createAlias(alias, toIndex)
    }

    fun allIndices(): List<IndexInfo> {
        val reader = BufferedReader(kerch.esClient
                .lowLevelClient
                .performRequest(Request("GET", "/_cat/indices?v&s=index"))
                .entity
                .content
                .reader())

        // Skip header
        reader.readLine()

        val indices = mutableListOf<IndexInfo>()
        reader.use { reader ->
            var line = reader.readLine()
            while (line != null) {
                val elements = line.split("\\s+".toRegex())
                indices.add(IndexInfo(elements[2], elements[6].toLong(), elements[8]))
                line = reader.readLine()
            }
        }
        return indices
    }
}

data class IndexInfo(val name: String,
                     val docCount: Long,
                     val size: String)