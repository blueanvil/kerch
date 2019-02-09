package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.nestie.DocType

/**
 * @author Cosmin Marginean
 */
@DocType(index = "data", type = "blog-entry")
data class BlogEntry(val title: String,
                     val tags: Set<String>) : ElasticsearchDocument()