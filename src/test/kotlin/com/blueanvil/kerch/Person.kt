package com.blueanvil.kerch

import com.github.javafaker.Faker

data class Person(var name: String,
                  var age: Int,
                  var gender: TestBase.Gender,
                  var id: String = uuid(),
                  var seqNo: Long = 0) {

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(TestBase.Gender::class.java))
}