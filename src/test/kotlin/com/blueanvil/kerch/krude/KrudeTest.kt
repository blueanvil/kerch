package com.blueanvil.kerch.krude

import com.blueanvil.kerch.*
import com.blueanvil.kerch.krude.model.*
import mbuhot.eskotlin.query.term.term
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
open class KrudeTest : TestBase() {

    @Test
    fun indexAndGet() {
        val uuid = uuid()
        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"), { "${it}_indexandget_$uuid" })
        val krude = krudes.forType(Publication::class)
        Assert.assertEquals("content_indexandget_$uuid", krude.index)

        val value = publicaton()
        val id = krude.save(value)
        waitToExist(krude.index, id)
        val newValue = krude.get(id)!!
        assertSamePublication(value, newValue)
    }


    @Test
    fun indexAndSearch() {
        val uuid = uuid()
        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"), { "${it}_indexandsearch_$uuid" })
        val krude = krudes.forType(BlogEntry::class)
        createTemplate("template-blogentry", krude.index)
        val ids = hashSetOf<String>()
        repeat(100) {
            ids.add(krude.save(blogEntry()))
        }

        wait("Indexing not finished") { kerch.search(krude.index).docCount() == 100L }
        val dog = term { krude.field("tags") to "DOG" }
        val dogs = term { krude.field("tags") to "DOGS" }

        ids.forEach {
            Assert.assertNotNull(krude.get(it))
        }

        Assert.assertTrue(krude.request().setQuery(dog).count() > 0)
        Assert.assertTrue(krude.request().setQuery(dogs).count() > 0)
        Assert.assertEquals(100, krude.request().setQuery(dog).count() + krude.request().setQuery(dogs).count())
        Assert.assertEquals(100, krude.find(dog).count() + krude.find(dogs).count())
        Assert.assertEquals(krude.find(dog).count().toLong(), krude.request().setQuery(dog).count())
        Assert.assertEquals(krude.find(dogs).count().toLong(), krude.request().setQuery(dogs).count())

        krude.request()
                .paging(10, 100)

    }

    fun publicaton(): Publication {
        return if (faker.random().nextLong() % 2 == 0L) {
            Magazine(faker.book().title(), faker.book().publisher())
        } else {
            Tabloid(faker.book().title(), faker.book().publisher(), faker.options().option(AudienceType::class.java))
        }
    }

    fun blogEntry(): BlogEntry {
        val secondTag = faker.options().option("DOG", "DOGS")
        return BlogEntry(faker.shakespeare().hamletQuote(), setOf("SHAKESPEARE", secondTag))
    }
}