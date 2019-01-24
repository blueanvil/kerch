package com.blueanvil.kerch.krude

import com.blueanvil.kerch.SamplePojo
import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.krude.model.Dog
import com.blueanvil.kerch.krude.model.Horse
import com.blueanvil.kerch.krude.model.Kingdom
import com.fasterxml.jackson.databind.module.SimpleModule
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class KrudeSerializationTest : TestBase() {

    @Test
    fun simpleSerialization() {
        val index = "krude"

        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
        val krude = krudes.forType(SamplePojo::class)
        val id = krude.save(randomPojo())
        waitToExist(index, id)
        Assert.assertNotNull(krude.get(id))
    }

    @Test
    fun indexSubType() {
        val index = "topbottom"
        createTemplate("template-krude", index)

        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
        val krude = krudes.forType(Top::class)

        val krudeObject = Bottom("George", mutableSetOf("admin"))
        val id = krude.save(krudeObject)
        waitToExist(index, id)
        Assert.assertNotNull(krude.get(id))
    }

    @Test
    fun customSerializer() {
        val index = "topbottom"
        createTemplate("template-krude", index)

        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
        val module = SimpleModule()
        module.addSerializer(BottomDog::class.javaObjectType, DogSerializer())
        module.addDeserializer(BottomDog::class.javaObjectType, DogDeserializer())
        krudes.addSerializationModule(module)

        val krude = krudes.forType(Top::class)

        val bottom = Bottom("George", mutableSetOf("admin"))
        bottom.someInt = 100
        bottom.dog = BottomDog("Winston")
        bottom.properties["Aaa"] = "BBB"
        val id = krude.save(bottom)
        waitToExist(index, id)

        Assert.assertNotNull(krude.get(id))
        Assert.assertEquals(100, (krude.get(id) as Bottom).someInt)
        Assert.assertEquals("Winston", (krude.get(id) as Bottom).dog?.name)

        val json = JSONObject(kerch.search(index).get(id).sourceAsString)
        Assert.assertTrue(json.has("random"))
        Assert.assertTrue(json.getJSONObject("random").get("someInt") is String)
        Assert.assertEquals("100nothing", json.getJSONObject("random").get("someInt"))
        Assert.assertEquals("Winston.nothing", json.getJSONObject("random").get("dog"))
    }

    @Test
    fun polymorphicCollection() {
        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
        val kingdom = Kingdom()
        kingdom.animals.add(Dog("Churchill"))
        kingdom.animals.add(Horse("Black Beauty"))
        println(krudes.kerch.toJson(kingdom))
    }
}