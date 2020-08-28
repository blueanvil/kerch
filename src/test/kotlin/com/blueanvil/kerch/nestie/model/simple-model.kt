package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.nestie.NestieDoc

/**
 * @author Cosmin Marginean
 */
@NestieDoc(type = "blog-entry")
data class BlogEntry(val title: String,
                     val tags: Set<String>,
                     val category: String? = null) : ElasticsearchDocument()

@NestieDoc(type = "blog-entry-custom-index")
data class BlogEntryCustomIndex(val title: String,
                                val tags: Set<String>) : ElasticsearchDocument()