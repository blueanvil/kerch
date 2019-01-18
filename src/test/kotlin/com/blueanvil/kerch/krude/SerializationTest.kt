package com.blueanvil.kerch.krude

import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class SerializationTest : KrudeTest() {

    @Test
    fun readWriteObject() {
        val krudes = Krudes(kerch, listOf("com.blueanvil.kerch.krude")) { "${it}_readwriteobject" }

        val krudeObject = randomPojo()
        val jsonStr = krudes.toJson(krudeObject)
        val krudeObject2 = krudes.fromJson(jsonStr, SamplePojo::class)

        Assert.assertEquals(krudeObject.id, krudeObject2.id)
        Assert.assertEquals(krudeObject.version, krudeObject2.version)
        Assert.assertEquals(krudeObject.name, krudeObject2.name)
        Assert.assertEquals(krudeObject.width, krudeObject2.width)
        Assert.assertEquals(krudeObject.height, krudeObject2.height)
        Assert.assertEquals(krudeObject, krudeObject2)
    }

}