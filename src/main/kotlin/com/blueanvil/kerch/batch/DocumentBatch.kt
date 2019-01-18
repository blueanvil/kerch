package com.blueanvil.kerch.batch

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.index.Indexer

/**
 * @author Cosmin Marginean
 */
class DocumentBatch<T : Document> internal constructor(indexer: Indexer,
                                                       size: Int = 100,
                                                       afterIndex: ((Collection<T>) -> Unit)? = null) : BatchBase<T>({ indexer.index(it) }, size, afterIndex)
