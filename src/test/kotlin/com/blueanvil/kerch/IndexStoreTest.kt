package com.blueanvil.kerch

import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.testng.Assert
import org.testng.Assert.assertEquals
import org.testng.Assert.assertNotNull
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
class IndexStoreTest : TestBase() {

    @Test
    fun updateField() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        val person = Person("John", 21, Gender.FEMALE)
        store.index(person, true)
        store.updateField(person.id, "age", 32, true)
        store.updateField(person.id, "gender", Gender.MALE, true)

        assertEquals(32, store.get(person.id, Person::class)!!.age)
        assertEquals(Gender.MALE, store.get(person.id, Person::class)!!.gender)
    }

    @Test
    fun updateInlinePainless() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        val person = Person("Jane", 46, Gender.FEMALE)
        store.index(person, true)
        store.updateWithPainlessScript(person.id, """
            ctx._source.age = params.newAge
        """, mapOf("newAge" to 66), true)

        assertEquals(66, store.get(person.id, Person::class)!!.age)
    }

    @Test
    fun updateByQuery() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        val person = Person("Max", 23, Gender.MALE)
        store.index(person, true)
        store.updateByQuery(termQuery("gender", Gender.MALE.name), """
            ctx._source.age = params.newAge
        """, mapOf("newAge" to 29))
        Thread.sleep(1)
        assertEquals(29, store.get(person.id, Person::class)!!.age)
    }


    @Test
    fun findOne() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        store.index(Person("Max", 23, Gender.MALE), true)
        store.index(Person("Jane", 45, Gender.FEMALE), true)
        store.index(Person("Andrew", 27, Gender.MALE), true)

        assertNotNull(store.findOne(termQuery("age", 23)))
        assertNotNull(store.findOne(termQuery("age", 45)))
        assertNotNull(store.findOne(termQuery("age", 27)))

        assertEquals("Max", store.findOne(termQuery("age", 23), Person::class)!!.name)
        assertEquals("Jane", store.findOne(termQuery("age", 45), Person::class)!!.name)
        assertEquals("Andrew", store.findOne(termQuery("age", 27), Person::class)!!.name)

        assertEquals("Max", store.findOne(termQuery("gender", Gender.MALE.name), Person::class, SortBuilders.fieldSort("age").order(SortOrder.ASC))!!.name)
        assertEquals("Andrew", store.findOne(termQuery("gender", Gender.MALE.name), Person::class, SortBuilders.fieldSort("age").order(SortOrder.DESC))!!.name)
    }

    @Test(expectedExceptions = [ActionRequestValidationException::class])
    fun versionConflict() {
        val index = peopleIndex()
        val store = kerch.store(index)

        val person = Person(faker)
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
    fun countByKeywordField() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()

        indexPeople(index, 100)
        val malesCount = store.count(termQuery("gender", "MALE"))
        val femalesCount = store.count(termQuery("gender", "FEMALE"))
        Assert.assertTrue(femalesCount > 0)
        Assert.assertTrue(malesCount > 0)
        assertEquals(100, malesCount + femalesCount)
    }

    @Test
    fun write() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 25)
        store.search(store.searchRequest().paging(0, 3), System.out)
    }

    @Test
    fun kerchTest() {
        val index = peopleIndex()
        val store = kerch.store(index) { "temp_$index" }

        indexPeople("temp_$index", 12)
        assertEquals(12, store.count())
        assertEquals(12, store.search(store.searchRequest().paging(0, 20)).size)
        assertEquals(12, super.count("temp_$index"))
    }
}