package com.blueanvil.kerch

import junit.framework.Assert.assertEquals
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class IndexMapperTest : TestBase() {

    @Test
    fun kerchTest() {
        val index = peopleIndex()
        val store = kerch.store(index) { "temp_$index" }

        indexPeople("temp_$index", 12)
        assertEquals(12, store.count())
        assertEquals(12, store.search(store.searchRequest().paging(0, 20)).size)
        assertEquals(12, super.count("temp_$index"))
    }
}