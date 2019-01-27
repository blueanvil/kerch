package com.blueanvil.kerch.batch

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.IndexStore
import java.util.*

/**
 * @author Cosmin Marginean
 */
class DocumentBatch<T : Document> internal constructor(private val store: IndexStore,
                                                       private val size: Int = 100,
                                                       private val afterIndex: ((Collection<T>) -> Unit)? = null) : AutoCloseable {
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
        store.index(documents)
        afterIndex?.invoke(documents)
        documents.clear()
    }

}
