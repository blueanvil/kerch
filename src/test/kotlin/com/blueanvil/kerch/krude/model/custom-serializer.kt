package com.blueanvil.kerch.krude.model

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType

/**
 * @author Cosmin Marginean
 */

@KrudeType(index = "content", type = "articles")
data class Article(val paragraphs: List<Paragraph>) : KrudeObject()

data class Paragraph(val length: Int,
                     val text: String)