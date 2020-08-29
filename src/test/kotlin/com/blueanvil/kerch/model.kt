package com.blueanvil.kerch

import com.github.javafaker.Faker

/**
 * @author Cosmin Marginean
 */
data class Person(var name: String,
                  var age: Int,
                  var gender: TestBase.Gender,
                  var id: String = uuid()) {

    var seqNo: Long = 0

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(TestBase.Gender::class.java))
}

data class PersonEs(var name: String,
                    var age: Int,
                    var gender: TestBase.Gender) : ElasticsearchDocument() {

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(TestBase.Gender::class.java))
}

data class PersonNoSeqNo(var name: String,
                         var age: Int,
                         var gender: TestBase.Gender,
                         var id: String = uuid()) {

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(TestBase.Gender::class.java))
}