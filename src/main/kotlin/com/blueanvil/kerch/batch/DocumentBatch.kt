package com.blueanvil.kerch.batch

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.IndexStoreBase
import java.util.*

/**
 * @author Cosmin Marginean
 */
class DocumentBatch<T : ElasticsearchDocument> internal constructor(private val store: IndexStoreBase<*>,
                                                                    private val size: Int = 100,
                                                                    private val waitRefresh: Boolean = false,
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
        store.index(documents, waitRefresh)
        afterIndex?.invoke(documents)
        documents.clear()
    }
}
