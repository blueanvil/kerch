package com.blueanvil.kerch

import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
class IndexWrapperTest : TestBase() {

    @Test
    fun moveIndex() {
        val alias = uuid()
        val indexWrapper = kerch.indexWrapper(alias)
        val index1 = indexWrapper.currentIndex
        wait("Index $index1 doesn't exist") { kerch.store(index1).indexExists }
        val index2 = indexWrapper.newIndexName()
        wait("Index $index2 doesn't exist") { kerch.store(index2).indexExists }
        indexWrapper.move(index2)
        wait("Index $index1 still exist") { !kerch.store(index1).indexExists }
        wait("Alias not moved") { indexWrapper.currentIndex == index2 }
    }
}