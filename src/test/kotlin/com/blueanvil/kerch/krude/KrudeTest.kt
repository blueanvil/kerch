package com.blueanvil.kerch.krude

import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.count
import mbuhot.eskotlin.query.term.term
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
open class KrudeTest : TestBase() {

    fun randomPojo(): SamplePojo {
        return SamplePojo(faker)
    }

    @Test
    fun indexAndGet() {
        val krudes = Krudes(kerch, listOf("com.blueanvil.kerch.krude")) { "${it}_indexandget" }

        val krude = krudes.forType(SamplePojo::class)
        val pojo = randomPojo()
        val id = krude.save(pojo)
        waitToExist(krude.index, id)
        val storedPojo = krude.get(id)
        Assert.assertEquals(pojo, storedPojo)
    }

    @Test
    fun indexAndSearch() {
        val krudes = Krudes(kerch, listOf("com.blueanvil.kerch.krude")) { "${it}_indexandsearch" }

        val krude = krudes.forType(SamplePojo::class)
        createTemplate("template-krude", krude.index)
        (1..100).asSequence().map {
            krude.save(randomPojo())
        }.toList().forEach {
            waitToExist(krude.index, it)
        }

        val c1 = krude.request().setQuery(term { krude.field("type") to "HUMAN" }).count()
        val c2 = krude.request().setQuery(term { krude.field("type") to "HUMANS" }).count()
        Assert.assertEquals(100, c1 + c2)
    }
}