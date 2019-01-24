package com.blueanvil.kerch.krude

import com.blueanvil.kerch.SamplePojo
import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.count
import com.blueanvil.kerch.wait
import mbuhot.eskotlin.query.term.term
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
open class KrudeTest : TestBase() {

    @Test
    fun indexAndGet() {
        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"), { "${it}_indexandget" })

        val krude = krudes.forType(SamplePojo::class)
        val pojo = randomPojo()
        val id = krude.save(pojo)
        waitToExist(krude.index, id)
        val storedPojo = krude.get(id)
        Assert.assertEquals(pojo, storedPojo)
    }

    @Test
    fun indexAndSearch() {
        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"), { "${it}_indexandsearch" })

        val krude = krudes.forType(SamplePojo::class)
        createTemplate("template-krude", krude.index)
        val ids = hashSetOf<String>()
        repeat(100) {
            ids.add(krude.save(randomPojo()))
        }

        wait("Indexing not finished") { kerch.search(krude.index).docCount() == 100L }
        val isHuman = term { krude.field("type") to "HUMAN" }
        val isHumans = term { krude.field("type") to "HUMANS" }

        ids.forEach {
            Assert.assertNotNull(krude.get(it))
        }

        Assert.assertEquals(100, krude.request().setQuery(isHuman).count() + krude.request().setQuery(isHumans).count())
        Assert.assertEquals(100, krude.find(isHuman).count() + krude.find(isHumans).count())
        Assert.assertEquals(krude.find(isHuman).count().toLong(), krude.request().setQuery(isHuman).count())
        Assert.assertEquals(krude.find(isHumans).count().toLong(), krude.request().setQuery(isHumans).count())
    }

    @Test
    fun fieldsTheSame() {
        val krudes = Krudes("blueanvil", listOf("localhost:9300"), listOf("com.blueanvil.kerch.krude"), { "${it}_indexandsearch" })
        val krude = krudes.forType(SamplePojo::class)
        Assert.assertEquals(Krudes.field(SamplePojo::class, "name"), krude.field("name"))
        Assert.assertEquals(Krudes.field(SamplePojo::class, "name"), "samplepojo.name")
    }
}