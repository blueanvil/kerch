package com.blueanvil.kerch.batch

import com.blueanvil.kerch.index.Indexer

/**
 * @author Cosmin Marginean
 */
class IndexBatch internal constructor(indexer: Indexer,
                                      size: Int = 100) : BatchBase<String>({ indexer.indexRaw(it) }, size)