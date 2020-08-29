package com.blueanvil.kerch

/**
 * @author Cosmin Marginean
 */
abstract class ElasticsearchDocument(open var id: String = uuid(),
                                     open var seqNo: Long = 0)