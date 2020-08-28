package com.blueanvil.kerch.batch

import java.util.*

/**
 * @author Cosmin Marginean
 */
class DocumentBatch<T : Any>
internal constructor(private val size: Int = 100,
                     private val bulkIndexer: (Collection<T>) -> Unit,
                     private val afterIndex: ((Collection<T>) -> Unit)? = null) : AutoCloseable {

    private val documents = ArrayList<T>()

    fun add(document: T) {
        documents.add(document)
        if (documents.size == size) {
            bulkIndex()
        }
    }

    fun add(documents: Collection<T>) {
        documents.forEach { add(it) }
    }

    override fun close() {
        if (documents.size > 0) {
            bulkIndex()
        }
    }

    private fun bulkIndex() {
        bulkIndexer(documents)
        afterIndex?.invoke(documents)
        documents.clear()
    }
}
