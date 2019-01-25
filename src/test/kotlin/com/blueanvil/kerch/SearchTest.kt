package com.blueanvil.kerch

import mbuhot.eskotlin.query.term.term
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class SearchTest : TestBase() {

    @Test
    fun searchNoTerm() {
        val index = peopleIndex()
        kerch.index(index).create()
        val search = kerch.search(index)

        indexPeople(index, 100)
        assertEquals(100, search.request().count())
        assertEquals(100, search.request().allHits().count())
    }

    @Test
    fun scroll() {
        val index = peopleIndex()
        kerch.index(index).create()

        val numberOfDocs = 17689
        indexPeople(index, numberOfDocs)
        val search = kerch.search(index)
        assertEquals(numberOfDocs, search.request().scroll().count())
//        assertEquals(numberOfDocs, search.request().scroll().map { hit -> hit.id }.toSet().size)
    }

    @Test
    fun searchDocuments() {
        val index = peopleIndex()
        kerch.index(index).create()

        val people = indexPeople(index, 100)
        kerch.search(index)
                .request()
                .allHits()
//                .map { kerch.document(it, Person::class) }
//                .forEach { doc ->
//                    val match = people.find {
//                        doc.name == it.name
//                                && doc.age == it.age
//                                && doc.gender == it.gender
//                    }
//                    assertNotNull(match)
//                }
    }

    @Test
    fun templateGenderKeyword() {
        val index = peopleIndex()
        createTemplate("template-people", index)
        kerch.index(index).create()

        indexPeople(index, 100)
        val search = kerch.search(index)
        val malesCount = search.request().setQuery(term { "gender" to "MALE" }).count()
        val femalesCount = search.request().setQuery(term { "gender" to "FEMALE" }).count()
        assertEquals(100, malesCount + femalesCount)
    }
}