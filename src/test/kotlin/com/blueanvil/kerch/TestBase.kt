package com.blueanvil.kerch

import com.blueanvil.kerch.nestie.Nestie
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.javafaker.Faker
import khttp.get
import org.apache.commons.io.IOUtils
import org.json.JSONObject
import org.junit.Assert
import org.slf4j.LoggerFactory
import java.nio.charset.StandardCharsets

/**
 * @author Cosmin Marginean
 */
abstract class TestBase {

    companion object {
        private val log = LoggerFactory.getLogger(TestBase::class.java)
    }

    val faker = Faker()
    val kerch = Kerch(clusterName = "blueanvil",
            nodes = listOf("localhost:9300"),
            objectMapper = jacksonObjectMapper())

    val nestie = Nestie(clusterName = "blueanvil",
            nodes = listOf("localhost:9300"),
            packages = listOf("com.blueanvil"))

    fun indexPeople(index: String, numberOfDocs: Int = 100): List<Person> {
        var people: MutableList<Person> = ArrayList()
        kerch.store(index).batch<Person>().use { batch ->
            repeat(numberOfDocs) {
                val person = Person(faker)
                people.add(person)
                batch.add(person)
            }
        }
        wait("Indexing not finished for $numberOfDocs docs in index $index") { kerch.store(index).search().docCount() == numberOfDocs.toLong() }
        return people
    }

    fun peopleIndex(): String {
        val index = "testindex.people.${uuid()}"
        log.info("Random people index: $index")
        return index
    }

    fun count(index: String): Long {
        val get = get("http://localhost:9200/${index}/_count")
        if (get.statusCode == 200) {
            return JSONObject(get.text).getLong("count")
        }
        return 0
    }

    fun waitToExist(index: String, id: String) {
        wait("Index not finished") { kerch.store(index).get(id).isExists }
    }

    fun createTemplate(templateName: String, appliesTo: String) {
        val jsonContent = IOUtils.toString(SearchTest::class.java.getResourceAsStream("/$templateName.json"), StandardCharsets.UTF_8)
                .replace("APPLIESTO", appliesTo)
        kerch.admin.createTemplate("$templateName-$appliesTo", jsonContent)
    }

    fun randomPerson(id: String? = null): Person {
        val person = Person(faker)
        if (id != null) {
            person.id = id
        }
        return person
    }

    enum class Gender { MALE, FEMALE }

    fun assertSameJson(json1: String, json2: String) {
        Assert.assertEquals(JSONObject(json1).toString(), JSONObject(json2).toString())
    }
}
