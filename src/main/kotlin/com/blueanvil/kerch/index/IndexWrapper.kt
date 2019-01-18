package com.blueanvil.kerch.index

import com.blueanvil.kerch.Kerch
import com.blueanvil.kerch.scroll
import com.blueanvil.kerch.uuid
import org.slf4j.LoggerFactory

/**
 * @author Cosmin Marginean
 */
class IndexWrapper(private val kerch: Kerch,
                   private val alias: String) {

    val index: Index get() = kerch.index(currentIndex())

    init {
        if (!kerch.admin.aliasExists(alias)) {
            val indexName = newIndexName()
            kerch.index(indexName).create()
            kerch.admin.createAlias(alias, indexName)
        }
    }

    fun reIndex() {
        val oldIndex = currentIndex()
        val newIndex = newIndexName()
        kerch.index(newIndex).create()

        kerch.index(oldIndex).readOnly
        val search = kerch.search(oldIndex)
        kerch.indexer(newIndex).batch().use { batch ->
            search.request()
                    .scroll()
                    .forEach { batch.add(it.id, it.sourceAsString) }
        }

        kerch.admin.moveAlias(alias, oldIndex, newIndex)
        log.info("Re-assign alias $alias from $oldIndex to $newIndex")
        kerch.index(oldIndex).delete()
    }

    private fun currentIndex() = kerch.admin.indicesForAlias(alias).first()

    private fun newIndexName(): String {
        return "$alias.${uuid()}"
    }

    companion object {
        private val log = LoggerFactory.getLogger(IndexWrapper::class.java)
    }
}
