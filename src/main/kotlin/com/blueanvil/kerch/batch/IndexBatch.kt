package com.blueanvil.kerch.batch

import com.blueanvil.kerch.index.Indexer
import com.blueanvil.kerch.uuid

/**
 * @author Cosmin Marginean
 */
class IndexBatch internal constructor(private var indexer: Indexer,
                                      private var size: Int = 100,
                                      private var afterIndex: ((Collection<Pair<String, String>>) -> Unit)? = null) : AutoCloseable {

    private val documents = mutableMapOf<String, String>()

    fun add(id: String?, documentJson: String) {
        documents[id ?: uuid()] = documentJson
        if (documents.size == size) {
            bulkIndex()
        }
    }

    override fun close() {
        if (documents.isNotEmpty()) {
            bulkIndex()
        }
    }

    private fun bulkIndex() {
        indexer.indexRaw(documents)
        afterIndex?.invoke(documents.map { it.toPair() })
        documents.clear()
    }

}