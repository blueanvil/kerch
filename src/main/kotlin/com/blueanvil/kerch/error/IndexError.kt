package com.blueanvil.kerch.error

import org.elasticsearch.action.bulk.BulkResponse

/**
 * @author Cosmin Marginean
 */
class IndexError(message: String,
                 val docErrors: Collection<IndexErrorDocRef>?) : RuntimeException(message) {

    constructor(bulkResponse: BulkResponse) : this("Bulk index failure", errors(bulkResponse))

    data class IndexErrorDocRef(val index: String,
                                val id: String,
                                val errorMessage: String)

    companion object {
        private fun errors(bulkResponse: BulkResponse): Collection<IndexErrorDocRef> {
            return bulkResponse.items
                    .filter { it.isFailed }
                    .map { item ->
                        IndexErrorDocRef(item.index, item.id, item.failureMessage)
                    }
                    .toList()
        }
    }
}