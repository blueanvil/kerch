package com.blueanvil.kerch.krude

import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class SerializationTest : KrudeTest() {

    @Test
    fun readWriteObject() {
        val krudes = Krudes(kerch, listOf("com.blueanvil.kerch.krude"))

        val krudeObject = randomPojo()
        val jsonStr = krudes.toJson(krudeObject)
        val krudeObject2 = krudes.fromJson(jsonStr, SamplePojo::class)

        Assert.assertEquals(krudeObject.id, krudeObject2.id)
        Assert.assertEquals(krudeObject.version, krudeObject2.version)
        Assert.assertEquals(krudeObject.name, krudeObject2.name)
        Assert.assertEquals(krudeObject.width, krudeObject2.width)
        Assert.assertEquals(krudeObject.height, krudeObject2.height)
        Assert.assertEquals(krudeObject.type, krudeObject2.type)
        Assert.assertEquals(krudeObject, krudeObject2)
    }


    @Test
    fun readWriteSubType() {
        val krudes = Krudes(kerch, listOf("com.blueanvil.kerch.krude"))

        val krudeObject = Bottom("George")
        val jsonStr = krudes.toJson(krudeObject)
        val krudeObject2 = krudes.fromJson(jsonStr, Top::class)
        val krudeObject3 = krudes.fromJson(jsonStr, Bottom::class)

        Assert.assertEquals(krudeObject.id, krudeObject2.id)
        Assert.assertEquals(krudeObject.version, krudeObject2.version)
        Assert.assertEquals(krudeObject.name, (krudeObject2 as Bottom).name)
        Assert.assertEquals(krudeObject, krudeObject2)
        Assert.assertEquals(krudeObject, krudeObject3)
    }

}