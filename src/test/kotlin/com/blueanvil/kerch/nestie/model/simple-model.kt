package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.nestie.NestieDoc
import com.blueanvil.kerch.uuid

/**
 * @author Cosmin Marginean
 */
@NestieDoc(type = "blog-entry")
data class BlogEntry(val title: String,
                     val tags: Set<String>,
                     val category: String? = null,
                     var id: String = uuid(),
                     var seqNo: Long = 0)

@NestieDoc(type = "blog-entry-custom-index")
data class BlogEntryCustomIndex(val title: String,
                                val tags: Set<String>,
                                var id: String = uuid(),
                                var seqNo: Long = 0)