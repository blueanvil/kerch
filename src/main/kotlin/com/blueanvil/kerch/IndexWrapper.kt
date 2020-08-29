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

    fun reIndex() {
        val oldIndex = currentIndex()
        val newIndex = newIndexName()

        val newStore = kerch.store(newIndex)
        newStore.createIndex()
        val oldStore = kerch.store(oldIndex)
        oldStore.readOnly = true

        newStore.rawBatch().use { batch ->
            oldStore.scroll().forEach { batch.add(it.id, it.sourceAsString) }
        }

        kerch.admin.moveAlias(alias, oldIndex, newIndex)
        log.info("Re-assigned alias $alias from $oldIndex to $newIndex")
        oldStore.deleteIndex()
    }

    fun currentIndex() = kerch.admin.indicesForAlias(alias).first()

    fun newIndexName(): String {
        return "$alias.${uuid()}"
    }

    companion object {
        private val log = LoggerFactory.getLogger(IndexWrapper::class.java)
    }
}
