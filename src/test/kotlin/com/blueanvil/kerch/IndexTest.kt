package com.blueanvil.kerch

import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class IndexTest : TestBase() {

    @Test
    fun basicIndexing() {
        val indexName = peopleIndex()
        val document = randomPerson()

        val index = kerch.index(indexName)
        val id = index.indexer.index(document)
        waitToExist(indexName, id)
        val person = index.search.get(id, Person::class)
        Assert.assertEquals(document, person)
    }
}