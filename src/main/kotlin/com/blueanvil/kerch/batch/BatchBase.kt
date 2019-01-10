package com.blueanvil.kerch.batch

import com.blueanvil.kerch.Indexer
import java.util.*

/**
 * @author Cosmin Marginean
 */
abstract class BatchBase<T : Any> internal constructor(private val indexer: Indexer,
                                                       private val size: Int = 100) : AutoCloseable {

    val documents = ArrayList<T>()

    abstract fun doBulkIndex(docs: Collection<T>)

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
        doBulkIndex(documents)
        documents.clear()
    }
}
