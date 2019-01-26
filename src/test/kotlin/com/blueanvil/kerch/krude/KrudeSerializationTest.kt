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
        assertSamePublication(tabloid, krudes.toDocument<Tabloid>(tabloidStr))

        val magazine = Magazine("Xyz", "Facebook")
        magazine.monthly = true
        val magazineStr = krudes.toJson(magazine)
        assertSameJson(magazineStr, """{"publication":{"name":"Xyz","monthly":true,"publisher":"Facebook","id":"${magazine.id}","type":"magazine","version":0}}""")
        assertSamePublication(magazine, krudes.toDocument<Magazine>(magazineStr))
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

}