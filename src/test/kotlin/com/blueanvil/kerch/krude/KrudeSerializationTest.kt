package com.blueanvil.kerch.krude

import com.blueanvil.kerch.CustomIntDeserializer
import com.blueanvil.kerch.CustomIntSerializer
import com.blueanvil.kerch.TestBase
import com.fasterxml.jackson.databind.module.SimpleModule
import org.json.JSONObject
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class KrudeSerializationTest : TestBase() {

    @Test
    fun indexSubType() {
        val index = "topbottom"
        createTemplate("template-krude", index)

        val krudes = Krudes(kerch, listOf("com.blueanvil.kerch.krude"))
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

        val module = SimpleModule()
        module.addSerializer(Int::class.java, CustomIntSerializer())
        module.addDeserializer(Int::class.java, CustomIntDeserializer())
        kerch.addSerializationModule(module)

        val krudes = Krudes(kerch, listOf("com.blueanvil.kerch.krude"))
        val krude = krudes.forType(Top::class)

        val krudeObject = Bottom("George", mutableSetOf("admin"))
        krudeObject.someInt = 100
        val id = krude.save(krudeObject)
        waitToExist(index, id)

        Assert.assertNotNull(krude.get(id))
        Assert.assertEquals(100, (krude.get(id) as Bottom).someInt)

        val json = JSONObject(kerch.search(index).get(id).sourceAsString)
        Assert.assertTrue(json.has("type"))
        Assert.assertTrue(json.has("random"))
        Assert.assertTrue(json.getJSONObject("random").get("someInt") is String)
    }
}