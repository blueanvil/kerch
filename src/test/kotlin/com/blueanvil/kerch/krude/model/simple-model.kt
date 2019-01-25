package com.blueanvil.kerch.krude.model

import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType

/**
 * @author Cosmin Marginean
 */
@KrudeType(index = "data", type = "blog-entry")
data class BlogEntry(val title: String,
                     val tags: Set<String>) : KrudeObject()