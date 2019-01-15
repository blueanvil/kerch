package com.blueanvil.kerch.index

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.search.Search
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.common.settings.Settings
import org.slf4j.LoggerFactory

/**
 * @author Cosmin Marginean
 */
class Index(val kerch: Kerch,
            val name: String) {

    companion object {
        private val log = LoggerFactory.getLogger(Index::class.java)
        private const val INDEX_READONLY = "index.blocks.read_only"
    }

    val indexer: Indexer get() = kerch.indexer(name)
    val search: Search get() = kerch.search(name)

    fun create(shards: Int = 5) {
        if (!exists) {
            var request = CreateIndexRequest(name)

            val settings = Settings.builder().put("index.number_of_shards", shards)
            request = request.settings(settings)

            val response = kerch.esClient
                    .admin()
                    .indices()
                    .create(request)
                    .actionGet()
            kerch.checkResponse(response)
            val refresh = kerch.esClient
                    .admin()
                    .indices()
                    .prepareRefresh(name)
                    .execute()
                    .actionGet()
            if (refresh.successfulShards < 1) {
                //TODO: Better handling
                throw RuntimeException("Index fail in ${refresh.failedShards} shards.")
            }
            log.info("Created index: {}", name)
        } else {
            log.info("Index {} already exists. Skipping", name)
        }
    }

    val exists: Boolean
        get() {
            val response = kerch.esClient
                    .admin()
                    .indices()
                    .prepareExists(name)
                    .execute()
                    .actionGet()
            return response.isExists
        }

    fun delete() {
        val deleteRequest = DeleteIndexRequest(name)
        val response = kerch.esClient
                .admin()
                .indices()
                .delete(deleteRequest)
                .actionGet()
        kerch.checkResponse(response)
    }

    var readOnly: Boolean
        set(value) {
            val response = kerch.esClient
                    .admin()
                    .indices()
                    .prepareUpdateSettings(name)
                    .setSettings(Settings.builder().put(INDEX_READONLY, value))
                    .execute()
                    .actionGet()
            kerch.checkResponse(response)
        }
        get () {
            val setting = kerch.esClient
                    .admin()
                    .indices()
                    .prepareGetSettings(name)
                    .execute()
                    .actionGet()
                    .getSetting(name, INDEX_READONLY)
            return setting?.toBoolean() ?: false
        }
}