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
        kerch.index(index).readOnly = true
        wait("Index not read only") { kerch.index(index).readOnly }
    }

    @Test(expected = IndexError::class)
    fun readOnlyWrite() {
        val index = peopleIndex()
        indexPeople(index, 1)
        kerch.index(index).readOnly = true
        wait("Index not read only") { kerch.index(index).readOnly }
        indexPeople(index, 1)
    }

    @Test
    fun readOnlyOnOff() {
        val index = peopleIndex()
        indexPeople(index, 1)
        kerch.index(index).readOnly = true
        wait("Index not read only") { kerch.index(index).readOnly }
        kerch.index(index).readOnly = false
        wait("Index still read only") { !kerch.index(index).readOnly }
        indexPeople(index, 1)
    }
}