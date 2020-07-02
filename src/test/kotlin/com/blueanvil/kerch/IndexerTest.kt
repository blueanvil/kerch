package com.blueanvil.kerch

import org.elasticsearch.action.ActionRequestValidationException
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
class IndexerTest : TestBase() {

    @Test(expectedExceptions = [ActionRequestValidationException::class])
    fun versionConflict() {
        val index = peopleIndex()
        val store = kerch.store(index)

        val person = randomPerson()
        val id = store.index(person)
        waitToExist(index, id)

        val p1 = store.get(id, Person::class)!!
        assertEquals(0, p1.seqNo)
        store.index(p1)
        store.index(p1)
        wait("Person not indexed") { store.get(id, Person::class)!!.seqNo == 2L }

        p1.seqNo = 1
        store.index(p1)
    }
}