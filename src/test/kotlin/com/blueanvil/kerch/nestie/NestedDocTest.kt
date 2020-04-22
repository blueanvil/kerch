package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.nestie.model.*
import com.blueanvil.kerch.wait
import mbuhot.eskotlin.query.term.term
import org.junit.Assert.*
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
open class NestedDocTest : TestBase() {

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
        val isDog = term { store.field("tags") to "DOG" }
        val isDogs = term { store.field("tags") to "DOGS" }

        ids.forEach {
            assertNotNull(store.get(it))
        }

        assertTrue(store.count(isDog) > 0)
        assertTrue(store.count(isDogs) > 0)
        assertEquals(100, store.count(isDog) + store.count(isDogs))
    }

    @Test
    fun indexAndSearchCustomIndex() {
        val indexName = "index-and-search-custom-index"
        val store = nestie.store(BlogEntryCustomIndex::class, indexName)
        createTemplate("template-blogentry", store.indexName)
        val ids = hashSetOf<String>()
        repeat(100) {
            ids.add(store.save(blogEntryCustomIndex()))
        }

        wait("Indexing not finished") { store.count() == 100L }
        val isDog = term { store.field("tags") to "DOG" }
        val isDogs = term { store.field("tags") to "DOGS" }

        ids.forEach {
            assertNotNull(store.get(it))
        }

        assertEquals(100, kerch.store(indexName).count())

        assertTrue(store.count(isDog) > 0)
        assertTrue(store.count(isDogs) > 0)
        assertEquals(100, store.count(isDog) + store.count(isDogs))
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