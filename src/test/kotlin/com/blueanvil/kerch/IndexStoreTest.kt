package com.blueanvil.kerch

import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class IndexStoreTest : TestBase() {

    @Test
    fun hitCount() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 200)

        val req = store.searchRequest().paging(0, 3)
        Assert.assertEquals(3, store.search(req).size)

        Assert.assertEquals(200, store.scroll().count())
    }

    @Test
    fun ids() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 100)
        Assert.assertEquals(100, store.allIds(store.searchRequest()).count())
    }

    @Test
    fun write() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 25)
        store.search(store.searchRequest().paging(0, 3), System.out)
    }
}