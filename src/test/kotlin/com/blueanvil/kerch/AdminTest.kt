package com.blueanvil.kerch

import com.blueanvil.kerch.error.IndexError
import junit.framework.Assert.assertTrue
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
class AdminTest : TestBase() {

    @Test
    fun readOnlyIndex() {
        val index = peopleIndex()
        indexPeople(index, 1)
        val store = kerch.store(index)
        store.readOnly = true
        wait("Index not read only") { store.readOnly }
    }

    @Test(expectedExceptions = [IndexError::class])
    fun readOnlyWrite() {
        val index = peopleIndex()
        indexPeople(index, 1)
        val store = kerch.store(index)
        store.readOnly = true
        wait("Index not read only") { store.readOnly }
        indexPeople(index, 1)
    }

    @Test
    fun readOnlyOnOff() {
        val index = peopleIndex()
        indexPeople(index, 1)
        val store = kerch.store(index)

        store.readOnly = true
        wait("Index not read only") { store.readOnly }
        store.readOnly = false
        wait("Index still read only") { !store.readOnly }
        indexPeople(index, 1)
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