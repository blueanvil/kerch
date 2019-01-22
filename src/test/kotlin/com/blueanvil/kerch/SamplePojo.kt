package com.blueanvil.kerch

import com.blueanvil.kerch.krude.ChildPojo
import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType
import com.blueanvil.kerch.krude.SampleType
import com.github.javafaker.Faker

/**
 * @author Cosmin Marginean
 */
@KrudeType(index = "krude", type = "samplepojo")
data class SamplePojo(var name: String,
                      var height: Int,
                      var width: Int,
                      var type: SampleType,
                      var child: ChildPojo) : KrudeObject() {
    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(0, 1000),
            faker.number().numberBetween(0, 1000),
            faker.options().option(SampleType::class.java),
            ChildPojo(faker))
}