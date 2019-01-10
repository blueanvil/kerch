package com.blueanvil.kerch.batch

import com.blueanvil.kerch.Indexer

/**
 * @author Cosmin Marginean
 */
class IndexBatch internal constructor(private val indexer: Indexer,
                                      private val size: Int = 100) : BatchBase<String>(indexer, size) {

    override fun doBulkIndex(docs: Collection<String>) {
        indexer.indexRaw(docs)
    }
}