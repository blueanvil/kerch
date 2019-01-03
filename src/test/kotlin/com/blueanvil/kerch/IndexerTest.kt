package com.blueanvil.kerch

import org.elasticsearch.index.engine.VersionConflictEngineException
import org.junit.Assert
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class IndexerTest : TestBase() {

    @Test(expected = VersionConflictEngineException::class)
    fun versionConflict() {
        val index = peopleIndex()
        val indexer = kerch.indexer(index)
        val search = kerch.search(index)

        val person = randomPerson()
        val id = indexer.index(person)
        waitToExist(index, id)

        val p1 = search.get(id).toDocument(Person::class)!!
        Assert.assertEquals(1, p1.version)
        indexer.index(p1)
        wait("Person not indexed") { search.get(id).toDocument(Person::class)!!.version == 2L }

        indexer.index(p1)
    }
}