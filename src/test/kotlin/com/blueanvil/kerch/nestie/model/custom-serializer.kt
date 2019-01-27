package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.Document
import com.blueanvil.kerch.nestie.DocType

/**
 * @author Cosmin Marginean
 */

@DocType(index = "content", type = "articles")
data class Article(val paragraphs: List<Paragraph>) : Document()

data class Paragraph(val length: Int,
                     val text: String)