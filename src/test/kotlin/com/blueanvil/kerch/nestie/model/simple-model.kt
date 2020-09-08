package com.blueanvil.kerch.nestie.model

import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.nestie.NestieDoc
import com.blueanvil.kerch.uuid
import com.github.javafaker.Faker

/**
 * @author Cosmin Marginean
 */
@NestieDoc(type = "blog-entry")
data class BlogEntry(val title: String,
                     val tags: Set<String>,
                     val category: String? = null,
                     var id: String = uuid(),
                     var version: Long = 0) {

    constructor(faker: Faker) : this(TestBase.faker.shakespeare().hamletQuote(), setOf("SHAKESPEARE", TestBase.faker.options().option("DOG", "DOGS")))
}
