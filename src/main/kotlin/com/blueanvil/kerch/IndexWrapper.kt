package com.blueanvil.kerch

import org.elasticsearch.client.RequestOptions
import org.elasticsearch.index.reindex.ReindexRequest
import org.slf4j.LoggerFactory

/**
 * @author Cosmin Marginean
 */
class IndexWrapper(private val kerch: Kerch,
                   val alias: String) {

    init {
        if (!kerch.admin.aliasExists(alias)) {
            val indexName = newIndexName()
            kerch.store(indexName).createIndex()
            kerch.admin.createAlias(alias, indexName)
        }
    }

    val currentIndex: String
        get() = kerch.admin.indicesForAlias(alias).first()

    fun newIndexName(createIndex: Boolean = true): String {
        val newIndex = "$alias.${uuid()}"
        if (createIndex) {
            kerch.store(newIndex).createIndex()
        }
        return newIndex
    }

    fun moveDataToNewIndex() {
        val oldIndex = currentIndex
        val newIndex = newIndexName()

        // Move data
        val docCount = kerch.store(oldIndex).count()
        log.info("Moving $docCount documents from $oldIndex to $newIndex")
        val reindexRequest = ReindexRequest()
        reindexRequest.setSourceIndices(oldIndex)
        reindexRequest.setDestIndex(newIndex)
        reindexRequest.isRefresh = true
        kerch.esClient.reindex(reindexRequest, RequestOptions.DEFAULT)
        log.info("Finished moving $docCount documents from $oldIndex to $newIndex")

        moveAlias(newIndex)
    }

    fun moveAlias(newIndex: String, deleteOldIndex: Boolean = true) {
        val oldIndex = currentIndex
        kerch.admin.moveAlias(alias, oldIndex, newIndex)
        if (deleteOldIndex) {
            kerch.store(oldIndex).deleteIndex()
        }
    }

    override fun toString(): String {
        return "IndexWrapper(alias='$alias', currentIndex='$currentIndex')"
    }


    companion object {
        private val log = LoggerFactory.getLogger(IndexWrapper::class.java)
    }
}
