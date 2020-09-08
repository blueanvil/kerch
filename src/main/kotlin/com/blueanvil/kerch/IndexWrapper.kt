package com.blueanvil.kerch

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

    fun move(newIndex: String, deleteOldIndex: Boolean = true) {
        val oldIndex = currentIndex
        kerch.admin.moveAlias(alias, oldIndex, newIndex)
        if (deleteOldIndex) {
            kerch.store(oldIndex).deleteIndex()
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(IndexWrapper::class.java)
    }
}
