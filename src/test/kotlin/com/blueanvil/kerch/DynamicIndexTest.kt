package com.blueanvil.kerch

import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class DynamicIndexTest : TestBase() {

    @Test
    fun indexPrefix() {
        var dynamicPrefix = "tenant1__"
        val kerch = Kerch(clusterName = "blueanvil", nodes = listOf("localhost:9300"), indexMapper = { index ->
            "${dynamicPrefix}$index"
        })

        val index = peopleIndex()
        kerch.indexer(index).index(Person(faker))
        wait("Document hasn't finished indexing") { count("tenant1__$index") == 1L }

        dynamicPrefix = "tenant2__"
        kerch.indexer(index).index(Person(faker))
        wait("Document hasn't finished indexing") { count("tenant2__$index") == 1L }

        Assert.assertEquals(1, count("tenant1__$index"))
    }
}