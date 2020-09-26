package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.*
import com.blueanvil.kerch.error.IndexError
import com.blueanvil.kerch.nestie.model.*
import org.elasticsearch.index.query.QueryBuilders.matchAllQuery
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.elasticsearch.search.sort.SortBuilders
import org.elasticsearch.search.sort.SortOrder
import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
open class NestieIndexStoreTest : TestBase() {

    @Test
    fun indexAndGet() {
        val store = nestie.store(Publication::class, "content-index-and-get")
        val value = publication()
        val id = store.save(value)
        waitToExist(store, id)
        val newValue = store.get(id)!!
        assertSamePublication(value, newValue)
    }

    @Test
    fun indexAndSearch() {
        val store = nestieStore(BlogEntry::class)
        val ids = hashSetOf<String>()
        repeat(100) {
            ids.add(store.save(BlogEntry(faker)))
        }

        wait("Indexing not finished") { store.count() == 100L }
        val nestieField = Nestie.field(BlogEntry::class, "tags")
        val isDog = termQuery(nestieField, "DOG")
        val isDogs = termQuery(nestieField, "DOGS")

        ids.forEach {
            assertNotNull(store.get(it))
        }

        assertTrue(store.count(isDog) > 0)
        assertTrue(store.count(isDogs) > 0)
        assertEquals(100, store.count(isDog) + store.count(isDogs))
    }

    @Test
    fun indexAndRawSearch() {
        val store = nestieStore(BlogEntry::class)
        val ids = hashSetOf<String>()
        repeat(100) {
            ids.add(store.save(BlogEntry(faker)))
        }

        wait("Indexing not finished") { store.count() == 100L }
        val nestieField = Nestie.field(BlogEntry::class, "tags")
        val isDog = termQuery(nestieField, "DOG")

        ids.forEach {
            assertNotNull(store.get(it))
        }

        val response = store.rawSearch(store.searchRequest().query(isDog))
        assertTrue(response.hits.hits.isNotEmpty())
    }

    @Test
    fun updateField() {
        val store = nestieStore(BlogEntry::class)
        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        store.updateField(id, Nestie.field(BlogEntry::class, "tags"), listOf("dance"), true)
        assertTrue(store.get(id)!!.tags.contains("dance"))
    }

    @Test
    fun countsAndScrollDefaults() {
        val store = nestieStore(BlogEntry::class)
        batchIndex(store, 139) { BlogEntry(faker) }
        assertEquals(store.count(), 139)
        assertEquals(store.scroll().count(), 139)
    }

    @Test
    fun deleteIndex() {
        val store = nestieStore(BlogEntry::class)
        store.createIndex()
        val be = BlogEntry(faker)
        store.save(be, true)
        assertTrue(store.exists(be.id))
        store.deleteIndex()
        assertFalse(kerch.admin.indexExists(store.indexName))
        assertFalse(store.indexExists)
    }

    @Test
    fun updateScript() {
        val store = nestieStore(BlogEntry::class)
        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        store.updateWithPainlessScript(id, """
            ctx._source["blog-entry"].tags = params.tags
        """, mapOf("tags" to listOf("knitting")), true)
        assertTrue(store.get(id)!!.tags.contains("knitting"))
    }

    @Test
    fun updateByQuery() {
        val store = nestieStore(BlogEntry::class)
        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        store.updateByQuery(matchAllQuery(), """
            ctx._source["blog-entry"].tags = params.tags
        """, mapOf("tags" to listOf("spelunking")))
        assertTrue(store.get(id)!!.tags.contains("spelunking"))
    }

    @Test
    fun delete() {
        val store = nestieStore(BlogEntry::class)
        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        assertNotNull(store.get(id))
        store.delete(id, true)
        assertNull(store.get(id))
    }

    @Test
    fun deleteByQuery() {
        val store = nestieStore(BlogEntry::class)
        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        assertNotNull(store.get(id))
        store.delete(termQuery(BlogEntry::class.nestieField("tags"), "stop"))
        assertNull(store.get(id))
    }

    @Test
    fun batchIndex() {
        val store = nestieStore(BlogEntry::class)
        val numberOfDocs = 234
        store.docBatch(13).use { batch ->
            repeat(numberOfDocs) {
                batch.add(BlogEntry(faker))
            }
        }
        Thread.sleep(1000)
        assertEquals(store.count(), numberOfDocs.toLong())

        store.docBatch(34, true).use { batch ->
            repeat(numberOfDocs) {
                batch.add(BlogEntry(faker))
            }
        }
        assertEquals(store.count(), numberOfDocs.toLong() * 2)
    }

    @Test
    fun findOne() {
        val store = nestieStore(BlogEntry::class)
        store.save(BlogEntry("Title1", setOf("t1"), "c1"), true)
        store.save(BlogEntry("Title2", setOf("t2"), "c2"), true)
        store.save(BlogEntry("Title3", setOf("t3"), "c1"), true)

        val catField = BlogEntry::class.nestieField("category")
        val tagField = BlogEntry::class.nestieField("tags")

        assertNotNull(store.findOne(termQuery(catField, "c1")))
        assertNotNull(store.findOne(termQuery(catField, "c2")))

        assertNull(store.findOne(termQuery(catField, "c4")))

        assertEquals("Title1", store.findOne(termQuery(catField, "c1"), tagField, SortOrder.ASC)!!.title)
        assertEquals("Title1", store.findOne(termQuery(catField, "c1"), SortBuilders.fieldSort(tagField).order(SortOrder.ASC))!!.title)
        assertEquals("Title3", store.findOne(termQuery(catField, "c1"), tagField, SortOrder.DESC)!!.title)
        assertEquals("Title3", store.findOne(termQuery(catField, "c1"), SortBuilders.fieldSort(tagField).order(SortOrder.DESC))!!.title)
    }

    @Test
    fun readOnlyIndex() {
        val store = store()
        batchIndex(store, 100) { Person(faker) }
        store.readOnly = true
        wait("Index not read only") { store.readOnly }
    }

    @Test(expectedExceptions = [IndexError::class])
    fun readOnlyWrite() {
        val store = nestieStore(BlogEntry::class)
        batchIndex(store, 1) { BlogEntry(faker) }
        store.readOnly = true
        wait("Index not read only") { store.readOnly }
        batchIndex(store, 1) { BlogEntry(faker) }
    }

    @Test
    fun readOnlyOnOff() {
        val store = nestieStore(BlogEntry::class)
        batchIndex(store, 1) { BlogEntry(faker) }

        store.readOnly = true
        wait("Index not read only") { store.readOnly }
        store.readOnly = false
        wait("Index still read only") { !store.readOnly }
        batchIndex(store, 1) { BlogEntry(faker) }
        Thread.sleep(1000)
        assertEquals(store.count(), 2)
    }

    @Test
    fun listIndices() {
        val store1 = nestieStore(BlogEntry::class)
        val store2 = nestieStore(BlogEntry::class)
        store1.createIndex()
        store2.createIndex()
        val names = kerch.admin.allIndices().map { it.name }
        assertTrue(names.contains(store1.indexName))
        assertTrue(names.contains(store2.indexName))
    }

    fun publication(): Publication {
        return if (faker.random().nextLong() % 2 == 0L) {
            Magazine(faker.book().title(), faker.book().publisher())
        } else {
            Tabloid(faker.book().title(), faker.book().publisher(), faker.options().option(AudienceType::class.java))
        }
    }
}