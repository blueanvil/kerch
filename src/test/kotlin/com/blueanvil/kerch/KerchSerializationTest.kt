package com.blueanvil.kerch

import com.blueanvil.kerch.krude.CustomIntDeserializer
import com.blueanvil.kerch.krude.CustomIntSerializer
import com.fasterxml.jackson.databind.module.SimpleModule
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class KerchSerializationTest : TestBase() {

    @Test
    fun customSerialization() {
        val module = SimpleModule()
        module.addSerializer(Int::class.java, CustomIntSerializer())
        module.addDeserializer(Int::class.java, CustomIntDeserializer())
        kerch.addSerializationModule(module)

        val index = peopleIndex()
        kerch.index(index).create()
        val search = kerch.search(index)

        val person = randomPerson()
        val id = kerch.indexer(index).index(person)
        waitToExist(index, id)
        Assert.assertTrue(search.get(id).sourceAsString.contains("\"age\":\"${person.age}nothing\""))
    }
}