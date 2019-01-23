package com.blueanvil.kerch.krude

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty
import com.github.javafaker.Faker

/**
 * @author Cosmin Marginean
 */
class ChildPojo
@JsonCreator constructor(@JsonProperty val age: Int?,
                         @JsonProperty val roles: MutableSet<String>?) {

    constructor(faker: Faker) : this(faker.number().numberBetween(20, 60), mutableSetOf(faker.job().position(), faker.job().position(), faker.job().position()))
}