package com.blueanvil.kerch.batch

import com.blueanvil.kerch.IndexStoreBase
import com.blueanvil.kerch.uuid

/**
 * @author Cosmin Marginean
 */
class IndexBatch internal constructor(private var store: IndexStoreBase<*>,
                                      private var size: Int = 100,
                                      private var waitRefresh: Boolean = false,
                                      private var afterIndex: ((Collection<Pair<String, String>>) -> Unit)? = null) : AutoCloseable {

    private val documents = mutableMapOf<String, String>()

    fun add(id: String?, documentJson: String) {
        documents[id ?: uuid()] = documentJson
        if (documents.size == size) {
            bulkIndex()
        }
    }

    fun add(documents: Map<String, String>) {
        documents.forEach { id, documentJson -> add(id, documentJson) }
    }

    override fun close() {
        if (documents.isNotEmpty()) {
            bulkIndex()
        }
    }

    private fun bulkIndex() {
        store.indexRaw(documents, waitRefresh)
        afterIndex?.invoke(documents.map { it.toPair() })
        documents.clear()
    }

}