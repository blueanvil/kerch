package com.blueanvil.kerch

import org.elasticsearch.index.query.QueryBuilders
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class ExtensionsTest : TestBase() {

    @Test
    fun hitCount() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 100)

        Assert.assertEquals(3, store.search()
                .paging(0, 3)
                .hits()
                .count())

        Assert.assertEquals(100, store.search()
                .scroll()
                .count())
    }

    @Test
    fun ids() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 100)
        Assert.assertEquals(10, store.search().ids().count())
        Assert.assertEquals(100, store.search(QueryBuilders.matchAllQuery()).ids().count())
    }
}