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

    companion object {
        private val log = LoggerFactory.getLogger(Admin::class.java)
        const val INDEX_READONLY = "index.blocks.read_only"
    }

    fun createTemplate(templateName: String, jsonContent: String) {
        val request = PutIndexTemplateRequest(templateName).source(jsonContent, XContentType.JSON)
        val response = kerch.esClient.admin()
                .indices()
                .execute(PutIndexTemplateAction.INSTANCE, request)
                .actionGet()
        kerch.checkResponse(response)
        log.info("Created template {}", templateName)
    }

    fun aliasExists(aliasName: String): Boolean {
        val response = kerch.esClient
                .admin()
                .indices()
                .prepareAliasesExist(aliasName)
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
        kerch.checkResponse(response)
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

    fun moveAlias(alias: String, fromIndex: String, toIndex: String) {
        val response = kerch.esClient
                .admin()
                .indices()
                .prepareAliases()
                .removeAlias(fromIndex, alias)
                .addAlias(toIndex, alias)
                .execute()
                .actionGet()
        kerch.checkResponse(response)
    }
}