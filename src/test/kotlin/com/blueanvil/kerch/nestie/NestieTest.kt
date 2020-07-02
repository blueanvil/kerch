package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.nestie.model.*
import com.blueanvil.kerch.wait
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.idsQuery
import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
open class NestieTest : TestBase() {

    @Test
    fun indexAndGet() {
        val store = nestie.store(Publication::class)
        val value = publicaton()
        val id = store.save(value)
        waitToExist(store.indexName, id)
        val newValue = store.get(id)!!
        assertSamePublication(value, newValue)
    }

    @Test
    fun indexAndSearch() {
        val store = nestie.store(BlogEntry::class)
        createTemplate("template-blogentry", store.indexName)
        val ids = hashSetOf<String>()
        repeat(100) {
            ids.add(store.save(blogEntry()))
        }

        wait("Indexing not finished") { store.count() == 100L }
        val nestieField = Nestie.field(BlogEntry::class, "tags")
        val isDog = QueryBuilders.termQuery(nestieField, "DOG")
        val isDogs = QueryBuilders.termQuery(nestieField, "DOGS")

        ids.forEach {
            assertNotNull(store.get(it))
        }

        assertTrue(store.count(isDog) > 0)
        assertTrue(store.count(isDogs) > 0)
        assertEquals(100, store.count(isDog) + store.count(isDogs))
    }

    @Test
    fun indexAndSearchCustomIndex() {
        val indexName = randomIndex("index-and-search-custom-index")
        val store = nestie.store(BlogEntryCustomIndex::class, indexName)
        createTemplate("template-blogentry", store.indexName)
        val ids = hashSetOf<String>()
        repeat(100) {
            ids.add(store.save(blogEntryCustomIndex()))
        }

        wait("Indexing not finished") { store.count() == 100L }
        val nestieField = Nestie.field(BlogEntryCustomIndex::class, "tags")
        val isDog = QueryBuilders.termQuery(nestieField, "DOG")
        val isDogs = QueryBuilders.termQuery(nestieField, "DOGS")

        ids.forEach {
            assertNotNull(store.get(it))
        }

        assertEquals(100, kerch.store(indexName).count())

        assertTrue(store.count(isDog) > 0)
        assertTrue(store.count(isDogs) > 0)
        assertEquals(100, store.count(isDog) + store.count(isDogs))
    }

    @Test
    fun updateField() {
        val indexName = randomIndex("update-field")
        val store = nestie.store(BlogEntry::class, indexName)
        createTemplate("template-blogentry", store.indexName)

        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        store.updateField(id, Nestie.field(BlogEntry::class, "tags"), listOf("dance"), true)
        assertTrue(store.get(id)!!.tags.contains("dance"))
    }

    @Test
    fun updateScript() {
        val indexName = randomIndex("update-by-script")
        val store = nestie.store(BlogEntry::class, indexName)
        createTemplate("template-blogentry", store.indexName)

        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        store.updateWithPainlessScript(id, """
            ctx._source["blog-entry"].tags = params.tags
        """, mapOf("tags" to listOf("knitting")), true)
        assertTrue(store.get(id)!!.tags.contains("knitting"))
    }

    @Test
    fun updateByQuery() {
        val indexName = randomIndex("update-by-query")
        val store = nestie.store(BlogEntry::class, indexName)
        createTemplate("template-blogentry", store.indexName)

        val doc = BlogEntry("Title", setOf("stop"))
        val id = store.save(doc, true)
        store.updateByQuery(idsQuery(id), """
            ctx._source["blog-entry"].tags = params.tags
        """, mapOf("tags" to listOf("spelunking")))
        assertTrue(store.get(id)!!.tags.contains("spelunking"))
    }

    fun publicaton(): Publication {
        return if (faker.random().nextLong() % 2 == 0L) {
            Magazine(faker.book().title(), faker.book().publisher())
        } else {
            Tabloid(faker.book().title(), faker.book().publisher(), faker.options().option(AudienceType::class.java))
        }
    }

    fun blogEntry(): BlogEntry {
        val secondTag = faker.options().option("DOG", "DOGS")
        return BlogEntry(faker.shakespeare().hamletQuote(), setOf("SHAKESPEARE", secondTag))
    }

    fun blogEntryCustomIndex(): BlogEntryCustomIndex {
        val secondTag = faker.options().option("DOG", "DOGS")
        return BlogEntryCustomIndex(faker.shakespeare().hamletQuote(), setOf("SHAKESPEARE", secondTag))
    }
}