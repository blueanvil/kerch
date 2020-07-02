package com.blueanvil.kerch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.javafaker.Faker
import org.apache.commons.io.IOUtils
import org.elasticsearch.index.query.QueryBuilders
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testng.Assert
import org.testng.annotations.Test
import java.nio.charset.StandardCharsets

/**
 * @author Cosmin Marginean
 */
class SampleTest {

//
//    @Test
//    fun testContainer() {
//        val container = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:7.6.2")
//
//        container.start()
//        val kerch = Kerch(nodes = listOf(container.httpHostAddress),
//                objectMapper = jacksonObjectMapper())
//
//        val index = "people"
//        fun createTemplate(templateName: String, appliesTo: String) {
//            val jsonContent = IOUtils.toString(SearchTest::class.java.getResourceAsStream("/$templateName.json"), StandardCharsets.UTF_8)
//                    .replace("APPLIESTO", appliesTo)
//            kerch.admin.createTemplate("$templateName-$appliesTo", jsonContent)
//        }
//        createTemplate("template-people", index)
//        val store = kerch.store(index)
//        store.createIndex()
//
//        indexPeople(kerch, index, 100)
//        val malesCount = store.count(QueryBuilders.termQuery("gender", "MALE"))
//        val femalesCount = store.count(QueryBuilders.termQuery("gender", "FEMALE"))
//        Assert.assertTrue(femalesCount > 0)
//        Assert.assertTrue(malesCount > 0)
//        Assert.assertEquals(100, malesCount + femalesCount)
//
//        container.close()
//    }
//
//    fun indexPeople(kerch: Kerch, index: String, numberOfDocs: Int = 100): List<Person> {
//        var people: MutableList<Person> = ArrayList()
//        kerch.store(index).docBatch<Person>().use { batch ->
//            repeat(numberOfDocs) {
//                val person = Person(Faker())
//                people.add(person)
//                batch.add(person)
//            }
//        }
//        wait("Indexing not finished for $numberOfDocs docs in index $index") { kerch.store(index).count() == numberOfDocs.toLong() }
//        return people
//    }
//

}