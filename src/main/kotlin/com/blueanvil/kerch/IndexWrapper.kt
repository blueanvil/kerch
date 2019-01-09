package com.blueanvil.kerch

/**
 * @author Cosmin Marginean
 */
class IndexWrapper(private val kerch: Kerch,
                   private val index: String) {

    init {
        if (!kerch.admin.aliasExists(index)) {
            val indexName = newIndexName()
            kerch.admin.createIndex(indexName)
            kerch.admin.createAlias(kerch.indexMapper(index), indexName)
        }
    }


    fun reindex() {
        val indexName = newIndexName()
        kerch.admin.createIndex(indexName)
        kerch.search(indexName)
    }

    private fun newIndexName(): String {
        return "${kerch.indexMapper(index)}.${uuid()}"
    }
}
