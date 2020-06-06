package com.blueanvil.kerch

/**
 * @author Cosmin Marginean
 */
abstract class ElasticsearchDocument(open var id: String = uuid(),
                                     var seqNo: Long = 0)