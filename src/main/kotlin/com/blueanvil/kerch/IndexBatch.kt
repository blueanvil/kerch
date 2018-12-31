package com.blueanvil.kerch

import java.util.*

/**
 * @author Cosmin Marginean
 */
class IndexBatch<T : Document> internal constructor(private val indexer: Indexer,
                                                    private val size: Int = 100) : AutoCloseable {

    private val documents = ArrayList<T>()

    fun add(document: T) {
        documents.add(document)
        if (documents.size == size) {
            bulkIndex()
        }
    }

    override fun close() {
        if (documents.size > 0) {
            bulkIndex()
        }
    }

    private fun bulkIndex() {
        indexer.index(documents)
        documents.clear()
    }
}
