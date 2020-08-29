package com.blueanvil.kerch

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.testng.Assert
import org.testng.annotations.Test
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class CoreFeaturesTest : TestBase() {

    @Test
    fun simpleDocument() = testCoreFeatures("template-person", Person::class) { randomPerson() }

    fun <T : Any> testCoreFeatures(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val baseName = docType.simpleName!!.toLowerCase()
        val index = randomIndex(baseName)
        createTemplate(templateName, baseName)
        val store = kerch.store(index)

        saveAndGet(newDoc, store, docType)
        exists(newDoc, store, docType)
        pagingAndHitCount(newDoc, store, docType)
    }

    private fun <T : Any> saveAndGet(newDoc: () -> T, store: IndexStore, docType: KClass<T>) {
        val document = newDoc()
        store.index(document)
        waitToExist(store, document.documentId)
        assertEquals(document, store.get(document.documentId, docType))
    }

    private fun <T : Any> exists(newDoc: () -> T, store: IndexStore, docType: KClass<T>) {
        val document = newDoc()
        store.index(document, true)
        assertTrue(store.exists(document.documentId))
    }

    private fun <T : Any> pagingAndHitCount(newDoc: () -> T, store: IndexStore, docType: KClass<T>) {
        batchIndex(store, 135, newDoc)

        Assert.assertEquals(store.search(store.searchRequest().paging(0, 3)).size, 3)
        Assert.assertEquals(store.search(store.searchRequest().paging(0, 10)).size, 10)
        Assert.assertEquals(store.scroll().count(), 135)
    }

    private fun <T : Any> allIdsCount(newDoc: () -> T, store: IndexStore, docType: KClass<T>) {
        batchIndex(store, 27, newDoc)
        assertEquals(store.allIds().count(), 27)
    }

    private fun <T : Any> batchIndex(store: IndexStore, numberOfDocs: Int, newDoc: () -> T) {
        store.rawBatch().use { batch ->
            repeat(numberOfDocs) {
                val doc = newDoc()
                batch.add(doc.documentId, kerch.toJson(doc))
            }
        }
        wait("Indexing not finished for $numberOfDocs docs in index ${store.indexName}") {
            println("${store.count()} vs ${numberOfDocs.toLong()}")
            store.count() == numberOfDocs.toLong()
        }
    }
}
