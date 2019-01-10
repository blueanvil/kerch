package com.blueanvil.kerch.batch

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.Indexer

/**
 * @author Cosmin Marginean
 */
class DocumentBatch<T : Document> internal constructor(private val indexer: Indexer,
                                                       private val size: Int = 100) : BatchBase<T>(indexer, size) {

    override fun doBulkIndex(docs: Collection<T>) {
        indexer.index(docs)
    }
}
