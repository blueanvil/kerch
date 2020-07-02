package com.blueanvil.kerch

import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
class IndexStoreTest : TestBase() {

    @Test
    fun hitCount() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 200)

        val req = store.searchRequest().paging(0, 3)
        assertEquals(3, store.search(req).size)

        assertEquals(200, store.scroll().count())
    }

    @Test
    fun ids() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 100)
        assertEquals(100, store.allIds().count())
    }

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
    fun write() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 25)
        store.search(store.searchRequest().paging(0, 3), System.out)
    }
}