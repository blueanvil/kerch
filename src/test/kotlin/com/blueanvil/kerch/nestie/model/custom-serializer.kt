package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.nestie.NestieDoc
import com.blueanvil.kerch.uuid

/**
 * @author Cosmin Marginean
 */

@NestieDoc(type = "articles")
data class Article(val paragraphs: List<Paragraph>,
                   var id: String = uuid(),
                   var version: Long = 0)

data class Paragraph(val length: Int,
                     val text: String)