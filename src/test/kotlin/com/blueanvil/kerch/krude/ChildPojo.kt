package com.blueanvil.kerch.krude

import com.github.javafaker.Faker

/**
 * @author Cosmin Marginean
 */
data class ChildPojo(val age: Int,
                     val roles: MutableSet<String>) {

    constructor(faker: Faker) : this(faker.number().numberBetween(20, 60), mutableSetOf(faker.job().position(), faker.job().position(), faker.job().position()))
}