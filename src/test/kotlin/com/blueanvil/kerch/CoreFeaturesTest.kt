package com.blueanvil.kerch

import org.elasticsearch.action.ActionRequestValidationException
import org.elasticsearch.search.sort.SortOrder
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
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

    @Test(expectedExceptions = [ActionRequestValidationException::class])
    fun conflictPerson() = conflict(Person::class) { Person(faker) }

    @Test(expectedExceptions = [ActionRequestValidationException::class])
    fun conflictPersonEs() = conflict(PersonEs::class) { PersonEs(faker) }

    fun conflictPersonNoSeqNo() = conflict(PersonNoSeqNo::class) { PersonNoSeqNo(faker) }

    @Test(expectedExceptions = [IllegalStateException::class])
    fun noIdField() {
        store().index(PersonWithNoId(faker))
    }

    private fun <T : Any> testCoreFeatures(templateName: String, docType: KClass<T>, sortField: String, newDoc: () -> T) {
        saveAndGet(docType, newDoc)
        exists(newDoc)
        pagingAndHitCount(newDoc)
        countForAllIds(newDoc)
        countsAndScrollDefaults(docType, newDoc)
        scroll(docType, newDoc)
        basicSort(sortField, newDoc)
        searchWithType(docType, newDoc)
        batchIndex(newDoc)
    }

    private fun <T : Any> saveAndGet(docType: KClass<T>, newDoc: () -> T) {
        val store = store()
        val document = newDoc()
        store.index(document)
        waitToExist(store, document.documentId)
        assertEquals(document, store.get(document.documentId, docType))
    }

    private fun <T : Any> exists(newDoc: () -> T) {
        val store = store()
        val document = newDoc()
        store.index(document, true)
        assertTrue(store.exists(document.documentId))
    }

    private fun <T : Any> pagingAndHitCount(newDoc: () -> T) {
        val store = store()
        batchIndex(store, 135, newDoc)

        assertEquals(store.search(store.searchRequest().paging(0, 3)).size, 3)
        assertEquals(store.search(store.searchRequest().paging(0, 10)).size, 10)
        assertEquals(store.scroll().count(), 135)
    }

    private fun <T : Any> countForAllIds(newDoc: () -> T) {
        val store = store()
        batchIndex(store, 27, newDoc)
        assertEquals(store.allIds().count(), 27)
    }

    private fun <T : Any> countsAndScrollDefaults(docType: KClass<T>, newDoc: () -> T) {
        val store = store()
        batchIndex(store, 139, newDoc)
        assertEquals(store.count(), 139)
        assertEquals(store.scroll().count(), 139)
        assertEquals(store.scroll(docType).count(), 139)
    }

    private fun <T : Any> scroll(docType: KClass<T>, newDoc: () -> T) {
        val store = store()
        val numberOfDocs = 17689
        batchIndex(store, numberOfDocs, newDoc)
        assertEquals(store.scroll(pageSize = 356).count(), numberOfDocs)
        assertEquals(store.scroll(pageSize = 173).map { hit -> hit.id }.toSet().size, numberOfDocs)
        assertEquals(store.scroll(docType, pageSize = 173).map { hit -> hit.documentId }.toSet().size, numberOfDocs)
    }

    private fun <T : Any> basicSort(sortField: String, newDoc: () -> T) {
        val store = store()
        batchIndex(store, 32, newDoc)

        val first = store.search(store.searchRequest().sort("$sortField.keyword", SortOrder.ASC)).first().sourceAsMap[sortField] as String
        val second = store.search(store.searchRequest().sort("$sortField.keyword", SortOrder.DESC)).first().sourceAsMap[sortField] as String
        assertTrue(first < second)
    }

    private fun <T : Any> searchWithType(docType: KClass<T>, newDoc: () -> T) {
        val store = store()
        val docs = batchIndex(store, 23, newDoc)
        assertEquals(store.search(store.searchRequest(), docType).size, 10)
        assertEquals(store.search(store.searchRequest().paging(0, 25), docType).toList().sortedBy { it.documentId }, docs.sortedBy { it.documentId })
    }

    private fun <T : Any> batchIndex(newDoc: () -> T) {
        val store = store()
        val numberOfDocs = 234
        store.batch(13).use { batch ->
            repeat(numberOfDocs) {
                val doc = newDoc()
                batch.add(doc.documentId, kerch.toJson(doc))
            }
        }
        Thread.sleep(1000)
        assertEquals(store.count(), numberOfDocs.toLong())

        store.docBatch<T>(34, true).use { batch ->
            repeat(numberOfDocs) {
                batch.add(newDoc())
            }
        }
        assertEquals(store.count(), numberOfDocs.toLong() * 2)
    }

    fun <T : Any> conflict(docType: KClass<T>, newDoc: () -> T) {
        val store = store()

        val doc = newDoc()
        val id = store.index(doc)
        waitToExist(store, id)

        val p1 = store.get(id, docType)!!
        assertEquals(0, p1.version)
        store.index(p1)
        store.index(p1)
        wait("Person not indexed") { store.get(id, Person::class)!!.version == 2L }

        p1.version = 1
        store.index(p1)
    }
}
