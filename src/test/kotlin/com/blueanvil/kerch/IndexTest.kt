package com.blueanvil.kerch

import org.testng.Assert.assertEquals
import org.testng.annotations.Test


/**
 * @author Cosmin Marginean
 */
class IndexTest : TestBase() {

    @Test
    fun basicIndexing() {
        val indexName = peopleIndex()
        val document = randomPerson()

        val store = kerch.store(indexName)
        val id = store.index(document)
        waitToExist(indexName, id)
        val person = store.get(id, Person::class)
        assertEquals(document, person)
    }
}