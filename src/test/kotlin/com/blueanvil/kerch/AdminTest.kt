package com.blueanvil.kerch

import com.blueanvil.kerch.error.IndexError
import org.testng.Assert.assertEquals
import org.testng.Assert.assertTrue
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
}