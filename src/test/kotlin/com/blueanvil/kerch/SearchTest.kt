package com.blueanvil.kerch

import mbuhot.eskotlin.query.term.term
import org.junit.Assert.*
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class SearchTest : TestBase() {

    @Test
    fun searchNoTerm() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()

        indexPeople(index, 100)
        assertEquals(100, store.search().docCount())
        assertEquals(100, store.search().allHits().count())
    }

    @Test
    fun scroll() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()

        val numberOfDocs = 17689
        indexPeople(index, numberOfDocs)
        assertEquals(numberOfDocs, store.search().scroll().count())
        assertEquals(numberOfDocs, store.search().scroll().map { hit -> hit.id }.toSet().size)
    }

    @Test
    fun searchDocuments() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()

        val people = indexPeople(index, 100)
        store.search()
                .allHits()
                .map { kerch.document(it, Person::class) }
                .forEach { doc ->
                    val match = people.find {
                        doc.name == it.name
                                && doc.age == it.age
                                && doc.gender == it.gender
                    }
                    assertNotNull(match)
                }
    }

    @Test
    fun templateGenderKeyword() {
        val index = peopleIndex()
        createTemplate("template-people", index)
        val store = kerch.store(index)
        store.createIndex()

        indexPeople(index, 100)
        val malesCount = store.search().setQuery(term { "gender" to "MALE" }).docCount()
        val femalesCount = store.search().setQuery(term { "gender" to "FEMALE" }).docCount()
        assertTrue(femalesCount > 0)
        assertTrue(malesCount > 0)
        assertEquals(100, malesCount + femalesCount)
    }
}