package com.blueanvil.kerch

import com.github.javafaker.Faker

/**
 * @author Cosmin Marginean
 */
data class Person(var name: String,
                  var age: Int,
                  var gender: Gender,
                  var id: String = uuid()) {

    var version: Long = 0

    constructor() : this("", 0, Gender.FEMALE)

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(Gender::class.java))
}

data class PersonEs(var name: String,
                    var age: Int,
                    var gender: Gender) : ElasticsearchDocument() {

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(Gender::class.java))
}

data class PersonNoSeqNo(var name: String,
                         var age: Int,
                         var gender: Gender,
                         var id: String = uuid()) {

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(Gender::class.java))
}

data class PersonWithNoId(var name: String,
                          var age: Int,
                          var gender: Gender) {

    constructor(faker: Faker) : this(faker.name().fullName(),
            faker.number().numberBetween(20, 80),
            faker.options().option(Gender::class.java))
}

enum class Gender { MALE, FEMALE }
