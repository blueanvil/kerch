package com.blueanvil.kerch

import com.blueanvil.kerch.error.IndexError
import org.junit.Test

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

    @Test(expected = IndexError::class)
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
        kerch.admin.allIndices().forEach { println(it) }
    }
}