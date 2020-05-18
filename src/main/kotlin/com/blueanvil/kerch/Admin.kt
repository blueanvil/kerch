package com.blueanvil.kerch

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequest.AliasActions
import org.elasticsearch.action.admin.indices.alias.get.GetAliasesRequest
import org.elasticsearch.action.admin.indices.template.put.PutIndexTemplateRequest
import org.elasticsearch.client.RequestOptions
import org.elasticsearch.common.xcontent.XContentType
import org.slf4j.LoggerFactory
import java.lang.UnsupportedOperationException
import java.util.*


/**
 * @author Cosmin Marginean
 */
class Admin(private val kerch: Kerch) {

    companion object {
        private val log = LoggerFactory.getLogger(Admin::class.java)
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
}