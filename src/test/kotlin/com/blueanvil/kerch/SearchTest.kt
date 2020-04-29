package com.blueanvil.kerch

import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.search.sort.SortOrder
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
        assertEquals(100, store.count())
        assertEquals(100, store.scroll().count())
    }

    @Test
    fun sort() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()

        indexPeople(index, 100)
        val first = store.search(store.searchRequest().sort("name.keyword", SortOrder.ASC)).first().sourceAsMap["name"] as String
        val second = store.search(store.searchRequest().sort("name.keyword", SortOrder.DESC)).first().sourceAsMap["name"] as String
        println("Comparing '$first' with '$second'")
        assertTrue(first < second)
    }

    @Test
    fun scroll() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()

        val numberOfDocs = 17689
        indexPeople(index, numberOfDocs)
        assertEquals(numberOfDocs, store.scroll().count())
        assertEquals(numberOfDocs, store.scroll().map { hit -> hit.id }.toSet().size)
    }

    @Test
    fun searchDocuments() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()

        val people = indexPeople(index, 100)
        store.search(store.searchRequest())
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
        val malesCount = store.count(QueryBuilders.termQuery("gender", "MALE"))
        val femalesCount = store.count(QueryBuilders.termQuery("gender", "FEMALE"))
        assertTrue(femalesCount > 0)
        assertTrue(malesCount > 0)
        assertEquals(100, malesCount + femalesCount)
    }
}