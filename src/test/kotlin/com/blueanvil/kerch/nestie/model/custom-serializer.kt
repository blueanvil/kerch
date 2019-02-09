package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.nestie.DocType

/**
 * @author Cosmin Marginean
 */

@DocType(index = "content", type = "articles")
data class Article(val paragraphs: List<Paragraph>) : ElasticsearchDocument()

data class Paragraph(val length: Int,
                     val text: String)