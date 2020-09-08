package com.blueanvil.kerch

import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.testng.Assert.*
import org.testng.annotations.Test
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class IndexStoreTest : TestBase() {

    @Test
    fun updateField() {
        val store = store()
        val person = Person("John", 21, Gender.FEMALE)
        store.index(person, true)
        store.updateField(person.id, "age", 32, true)
        store.updateField(person.id, "gender", Gender.MALE, true)

        assertEquals(32, store.get(person.id, Person::class)!!.age)
        assertEquals(Gender.MALE, store.get(person.id, Person::class)!!.gender)
    }

    @Test
    fun updateInlinePainless() {
        val store = store()
        val person = Person("Jane", 46, Gender.FEMALE)
        store.index(person, true)
        store.updateWithPainlessScript(person.id, """
            ctx._source.age = params.newAge
        """, mapOf("newAge" to 66), true)

        assertEquals(66, store.get(person.id, Person::class)!!.age)
    }

    @Test
    fun updateByQuery() {
        val store = store()
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
        val store = store()

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

    @Test
    fun searchDocuments() {
        val store = store()

        val people = batchIndex(store, 100) { Person(faker) }
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
        val store = store()
        batchIndex(store, 100) { Person(faker) }
        val malesCount = store.count(termQuery("gender", "MALE"))
        val femalesCount = store.count(termQuery("gender", "FEMALE"))
        assertTrue(femalesCount > 0)
        assertTrue(malesCount > 0)
        assertEquals(100, malesCount + femalesCount)
    }

    @Test
    fun write() {
        val store = store()
        store.createIndex()
        batchIndex(store, 25) { Person(faker) }
        store.search(store.searchRequest().paging(0, 3), System.out)
    }

    @Test
    fun deleteIndex() {
        val store = store()
        store.createIndex()
        val person = Person(faker)
        store.index(person, true)
        assertTrue(store.exists(person.id))
        store.deleteIndex()
        assertFalse(kerch.admin.indexExists(store.indexName))
    }
}