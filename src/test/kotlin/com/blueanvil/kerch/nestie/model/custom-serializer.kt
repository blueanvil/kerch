package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.nestie.NestieDoc

/**
 * @author Cosmin Marginean
 */

@NestieDoc(type = "articles")
data class Article(val paragraphs: List<Paragraph>) : ElasticsearchDocument()

data class Paragraph(val length: Int,
                     val text: String)