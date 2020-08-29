package com.blueanvil.kerch

import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertTrue
import org.elasticsearch.search.sort.SortOrder
import org.testng.Assert
import org.testng.annotations.Test
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class CoreFeaturesTest : TestBase() {

    @Test
    fun person() = testCoreFeatures("template-person", Person::class, "name") { Person(faker) }

    @Test
    fun personEs() = testCoreFeatures("template-person", PersonEs::class, "name") { PersonEs(faker) }

    @Test
    fun personEsNoSeqNo() = testCoreFeatures("template-person", PersonNoSeqNo::class, "name") { PersonNoSeqNo(faker) }

    private fun <T : Any> testCoreFeatures(templateName: String, docType: KClass<T>, sortField: String, newDoc: () -> T) {
        saveAndGet(templateName, docType, newDoc)
        exists(templateName, docType, newDoc)
        pagingAndHitCount(templateName, docType, newDoc)
        countForAllIds(templateName, docType, newDoc)
        countsAndScrollDefaults(templateName, docType, newDoc)
        scroll(templateName, docType, newDoc)
        basicSort(templateName, docType, sortField, newDoc)
        searchWithType(templateName, docType, newDoc)
        batchIndex(templateName, docType, newDoc)
    }

    private fun <T : Any> newStore(docType: KClass<T>, templateName: String): IndexStore {
        val baseName = docType.simpleName!!.toLowerCase()
        val index = randomIndex(baseName)
        createTemplate(templateName, baseName)
        val store = kerch.store(index)
        return store
    }

    private fun <T : Any> saveAndGet(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        val document = newDoc()
        store.index(document)
        waitToExist(store, document.documentId)
        assertEquals(document, store.get(document.documentId, docType))
    }

    private fun <T : Any> exists(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        val document = newDoc()
        store.index(document, true)
        assertTrue(store.exists(document.documentId))
    }

    private fun <T : Any> pagingAndHitCount(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        batchIndex(store, 135, newDoc)

        Assert.assertEquals(store.search(store.searchRequest().paging(0, 3)).size, 3)
        Assert.assertEquals(store.search(store.searchRequest().paging(0, 10)).size, 10)
        Assert.assertEquals(store.scroll().count(), 135)
    }

    private fun <T : Any> countForAllIds(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        batchIndex(store, 27, newDoc)
        assertEquals(store.allIds().count(), 27)
    }

    private fun <T : Any> countsAndScrollDefaults(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        batchIndex(store, 139, newDoc)
        Assert.assertEquals(store.count(), 139)
        Assert.assertEquals(store.scroll().count(), 139)
    }

    private fun <T : Any> scroll(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        val numberOfDocs = 17689
        batchIndex(store, numberOfDocs, newDoc)
        Assert.assertEquals(store.scroll(pageSize = 356).count(), numberOfDocs)
        Assert.assertEquals(store.scroll(pageSize = 173).map { hit -> hit.id }.toSet().size, numberOfDocs)
    }

    private fun <T : Any> basicSort(templateName: String, docType: KClass<T>, sortField: String, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        batchIndex(store, 32, newDoc)

        val first = store.search(store.searchRequest().sort("$sortField.keyword", SortOrder.ASC)).first().sourceAsMap[sortField] as String
        val second = store.search(store.searchRequest().sort("$sortField.keyword", SortOrder.DESC)).first().sourceAsMap[sortField] as String
        Assert.assertTrue(first < second)
    }

    private fun <T : Any> searchWithType(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        val docs = batchIndex(store, 23, newDoc)
        assertEquals(store.search(store.searchRequest(), docType).size, 10)
        assertEquals(store.search(store.searchRequest().paging(0, 25), docType).toList(), docs)
    }

    private fun <T : Any> batchIndex(templateName: String, docType: KClass<T>, newDoc: () -> T) {
        val store = newStore(docType, templateName)
        val numberOfDocs = 234
        store.rawBatch(13).use { batch ->
            repeat(numberOfDocs) {
                val doc = newDoc()
                batch.add(doc.documentId, kerch.toJson(doc))
            }
        }
        Thread.sleep(1000)
        assertEquals(store.count(), numberOfDocs.toLong())

        store.rawBatch(34, true).use { batch ->
            repeat(numberOfDocs) {
                val doc = newDoc()
                batch.add(doc.documentId, kerch.toJson(doc))
            }
        }
        assertEquals(store.count(), numberOfDocs.toLong() * 2)
    }

}
