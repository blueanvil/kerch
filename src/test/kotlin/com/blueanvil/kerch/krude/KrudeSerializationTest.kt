package com.blueanvil.kerch.krude

import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.krude.model.*
import com.fasterxml.jackson.databind.module.SimpleModule
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class KrudeSerializationTest : TestBase() {

    @Test
    fun simpleModel() {
        val value = BlogEntry("A day in December", setOf("java", "kotlin"))
        val jsonStr = krudes.toJson(value)
        assertSameJson(jsonStr, """{"blog-entry":{"title":"A day in December","tags":["java","kotlin"],"id":"${value.id}","version":0}}""")

        val newValue = krudes.toDocument<BlogEntry>(jsonStr)
        Assert.assertEquals(newValue, value)
    }

    @Test
    fun polymorphicObjects() {
        val tabloid = Tabloid("Abc", "Nüz of the world", AudienceType.ADULT)
        val tabloidStr = krudes.toJson(tabloid)
        assertSameJson(tabloidStr, """{"publication":{"audience":"ADULT","name":"Abc","publisher":"Nüz of the world","id":"${tabloid.id}","type":"tabloid","version":0}}""")

        val tabloid2 = krudes.toDocument<Tabloid>(tabloidStr)
        Assert.assertEquals(tabloid.id, tabloid2.id)
        Assert.assertEquals(tabloid.name, tabloid2.name)
        Assert.assertEquals(tabloid.publisher, tabloid2.publisher)
        Assert.assertEquals(tabloid.audience, tabloid2.audience)

        val magazine = Magazine("Xyz", "Facebook")
        magazine.monthly = true
        val magazineStr = krudes.toJson(magazine)
        assertSameJson(magazineStr, """{"publication":{"name":"Xyz","monthly":true,"publisher":"Facebook","id":"${magazine.id}","type":"magazine","version":0}}""")

        val magazine2 = krudes.toDocument<Magazine>(magazineStr)
        Assert.assertEquals(magazine2.id, magazine2.id)
        Assert.assertEquals(magazine2.name, magazine2.name)
        Assert.assertEquals(magazine2.publisher, magazine2.publisher)
        Assert.assertEquals(magazine2.monthly, magazine2.monthly)
    }

    @Test
    fun polymorphicChildren() {
        val animals = listOf(
                Dog("Winston", "Border Collie"),
                Horse("Black Beauty", 4)
        )
        val kingdom = Kingdom(animals)
        val jsonStr = krudes.toJson(kingdom)
        assertSameJson(jsonStr, """{"kingdom":{"animals":[{"type":"dog","name":"Winston","breed":"Border Collie"},{"type":"horse","name":"Black Beauty","age":4}],"id":"${kingdom.id}","version":0}}""")

        val kingdom2 = krudes.toDocument<Kingdom>(jsonStr)
        Assert.assertTrue(kingdom.animals[0] is Dog)
        Assert.assertTrue(kingdom2.animals[0] is Dog)
        Assert.assertEquals(kingdom.animals[0].name, "Winston")
        Assert.assertEquals(kingdom.animals[0].name, kingdom2.animals[0].name)
        Assert.assertEquals((kingdom.animals[0] as Dog).breed, "Border Collie")
        Assert.assertEquals((kingdom.animals[0] as Dog).breed, (kingdom2.animals[0] as Dog).breed)

        Assert.assertTrue(kingdom.animals[1] is Horse)
        Assert.assertTrue(kingdom2.animals[1] is Horse)
        Assert.assertEquals(kingdom.animals[1].name, "Black Beauty")
        Assert.assertEquals(kingdom.animals[1].name, kingdom2.animals[1].name)
        Assert.assertEquals((kingdom.animals[1] as Horse).age, 4)
        Assert.assertEquals((kingdom.animals[1] as Horse).age, (kingdom2.animals[1] as Horse).age)
    }

    @Test
    fun customSerializer() {
        val k = krudes

        val module = SimpleModule()
        module.addSerializer(Paragraph::class.java, ParagraphSerializer())
        module.addDeserializer(Paragraph::class.java, ParagraphDeserializer())
        k.addSerializationModule(module)

        val article = Article(listOf(
                Paragraph(11, "abc def ghi"),
                Paragraph(12, "ABC DEF GHI")
        ))
        val jsonStr = k.toJson(article)
        assertSameJson(jsonStr, """{"articles":{"paragraphs":["11:abc def ghi","12:ABC DEF GHI"],"id":"${article.id}","version":0}}""")
    }

//
//    @Test
//    fun simpleSerialization() {
//        val index = "krude"
//
//        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
//        val krude = krudes.forType(SamplePojo::class)
//        val id = krude.save(randomPojo())
//        waitToExist(index, id)
//        Assert.assertNotNull(krude.get(id))
//    }
//
//    @Test
//    fun indexSubType() {
//        val index = "topbottom"
//        createTemplate("template-krude", index)
//
//        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
//        val krude = krudes.forType(Top::class)
//
//        val krudeObject = Bottom("George", mutableSetOf("admin"))
//        val id = krude.save(krudeObject)
//        waitToExist(index, id)
//        Assert.assertNotNull(krude.get(id))
//    }
//
//    @Test
//    fun customSerializer() {
//        val index = "topbottom"
//        createTemplate("template-krude", index)
//
//        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
//        val module = SimpleModule()
//        module.addSerializer(BottomDog::class.javaObjectType, DogSerializer())
//        module.addDeserializer(BottomDog::class.javaObjectType, DogDeserializer())
//        krudes.addSerializationModule(module)
//
//        val krude = krudes.forType(Top::class)
//
//        val bottom = Bottom("George", mutableSetOf("admin"))
//        bottom.someInt = 100
//        bottom.dog = BottomDog("Winston")
//        bottom.properties["Aaa"] = "BBB"
//        val id = krude.save(bottom)
//        waitToExist(index, id)
//
//        Assert.assertNotNull(krude.get(id))
//        Assert.assertEquals(100, (krude.get(id) as Bottom).someInt)
//        Assert.assertEquals("Winston", (krude.get(id) as Bottom).dog?.name)
//
//        val json = JSONObject(kerch.search(index).get(id).sourceAsString)
//        Assert.assertTrue(json.has("random"))
//        Assert.assertTrue(json.getJSONObject("random").get("someInt") is String)
//        Assert.assertEquals("100nothing", json.getJSONObject("random").get("someInt"))
//        Assert.assertEquals("Winston.nothing", json.getJSONObject("random").get("dog"))
//    }
//
//    @Test
//    fun polymorphicCollection() {
//        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"))
//        val kingdom = Kingdom()
//        kingdom.animals.add(Dog("Churchill"))
//        kingdom.animals.add(Horse("Black Beauty"))
//        println(krudes.kerch.toJson(kingdom))
//    }
}