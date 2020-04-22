package com.blueanvil.kerch

import org.elasticsearch.index.query.QueryBuilders
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class KerchSearchRequestTest : TestBase() {

    @Test
    fun hitCount() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 200)

        Assert.assertEquals(3, store.search()
                .paging(0, 3)
                .hits()
                .count())

        Assert.assertEquals(200, store.search()
                .scroll()
                .count())
    }

    @Test
    fun ids() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 100)
        Assert.assertEquals(100, store.search().ids().count())
        Assert.assertEquals(100, store.search(QueryBuilders.matchAllQuery()).ids().count())
    }

    @Test
    fun write() {
        val index = peopleIndex()
        val store = kerch.store(index)
        store.createIndex()
        indexPeople(index, 25)
        store.search().paging(0, 3).write(System.out)
    }
}