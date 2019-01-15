package com.blueanvil.kerch

import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class IndexWrapperTest : TestBase() {

    @Test
    fun aliasReassign() {
        val wrapper = kerch.indexWrapper("my-wrapped-index-${uuid()}")
        val numberOfDocs = 874L
        indexPeople(wrapper.index.name, numberOfDocs.toInt())

        val oldIndex = wrapper.index.name
        Assert.assertTrue(wrapper.index.exists)
        Assert.assertEquals(numberOfDocs, wrapper.index.search.docCount())
        Assert.assertEquals(numberOfDocs, kerch.search(oldIndex).docCount())

        wrapper.reIndex()
        wait("Indexing in new index not finished") { kerch.search(wrapper.index.name).docCount() == numberOfDocs }

        Assert.assertFalse(kerch.index(oldIndex).exists)
        Assert.assertTrue(wrapper.index.exists)
        Assert.assertEquals(numberOfDocs, wrapper.index.search.docCount())
    }
}
