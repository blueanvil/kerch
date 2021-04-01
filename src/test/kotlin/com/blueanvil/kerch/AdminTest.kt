package com.blueanvil.kerch

import com.blueanvil.kerch.error.IndexError
import com.blueanvil.kerch.nestie.NestieDoc
import org.elasticsearch.index.query.QueryBuilders.matchQuery
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.testng.Assert.*
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
class AdminTest : TestBase() {

    @Test
    fun readOnlyIndex() {
        val store = store()
        batchIndex(store, 100) { Person(faker) }
        store.readOnly = true
        wait("Index not read only") { store.readOnly }
    }

    @Test(expectedExceptions = [IndexError::class])
    fun readOnlyWrite() {
        val store = store()
        batchIndex(store, 1) { Person(faker) }
        store.readOnly = true
        wait("Index not read only") { store.readOnly }
        batchIndex(store, 1) { Person(faker) }
    }

    @Test
    fun readOnlyOnOff() {
        val store = store()
        batchIndex(store, 1) { Person(faker) }

        store.readOnly = true
        wait("Index not read only") { store.readOnly }
        store.readOnly = false
        wait("Index still read only") { !store.readOnly }
        batchIndex(store, 1) { Person(faker) }
        assertEquals(store.count(), 1)
    }

    @Test
    fun listIndices() {
        val index1 = uuid()
        kerch.admin.createIndex(index1)

        val index2 = uuid()
        kerch.admin.createIndex(index2)

        val index3 = uuid()
        kerch.admin.createIndex(index3)

        val names = kerch.admin.allIndices().map { it.name }
        assertTrue(names.contains(index1))
        assertTrue(names.contains(index2))
        assertTrue(names.contains(index3))
    }

    @Test
    fun saveTemplateAndReindex() {
        val alias = "savetemplateandreindex"
        val templateName = "savetemplateandreindex-template"
        val template1 = resourceAsString("template-update1.json")
        val template2 = resourceAsString("template-update2.json")

        val indexWrapper = kerch.indexWrapper(alias)
        val index1 = indexWrapper.currentIndex

        kerch.admin.createTemplate(templateName, template1)
        nestie.store(TemplateUpdateTestDoc::class, alias)

        val nestieStore = nestie.store(TemplateUpdateTestDoc::class, alias)
        nestieStore.save(TemplateUpdateTestDoc("John Smith"), true)
        nestieStore.save(TemplateUpdateTestDoc("John Smith Michaels"), true)
        val nameField = TemplateUpdateTestDoc::class.nestieField("name")
        assertEquals(nestieStore.count(matchQuery(nameField, "John Smith")), 2)
        assertEquals(nestieStore.count(termQuery(nameField, "John Smith")), 0)
        assertEquals(nestieStore.count(termQuery(nameField, "John Smith Michaels")), 0)

        kerch.admin.saveTemplateAndReindex(templateName, template2)
        assertFalse(kerch.admin.indexExists(index1))
        assertTrue(kerch.admin.indexExists(indexWrapper.currentIndex))
        assertNotEquals(index1, indexWrapper.currentIndex)
        val newStore = nestie.store(TemplateUpdateTestDoc::class, alias)
        assertEquals(newStore.count(termQuery(nameField, "John Smith")), 1)
        assertEquals(newStore.count(termQuery(nameField, "John Smith Michaels")), 1)

        // Now test that we can update the template but no reindexing is done
        val beforeSecondUpdate = indexWrapper.currentIndex
        kerch.admin.saveTemplateAndReindex(templateName, template2)
        assertEquals(beforeSecondUpdate, indexWrapper.currentIndex)
    }
}

@NestieDoc(type = "template-update-test")
data class TemplateUpdateTestDoc(val name: String) : ElasticsearchDocument()