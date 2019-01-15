package com.blueanvil.kerch.batch

import java.util.*

/**
 * @author Cosmin Marginean
 */
abstract class BatchBase<T : Any> internal constructor(private val indexer: (Collection<T>) -> Unit,
                                                       private val size: Int = 100) : AutoCloseable {

    val documents = ArrayList<T>()

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
        indexer(documents)
        documents.clear()
    }
}
