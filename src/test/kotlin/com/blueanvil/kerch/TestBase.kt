package com.blueanvil.kerch

import com.github.javafaker.Faker
import khttp.get
import org.apache.commons.io.IOUtils
import org.json.JSONObject
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
    val kerch = Kerch("blueanvil", listOf("localhost:9300"))

    fun indexPeople(index: String, numberOfDocs: Int = 100): List<Person> {
        var people: MutableList<Person> = ArrayList()
        kerch.indexer(index).batch<Person>().use { batch ->
            repeat(numberOfDocs) {
                val person = Person(faker)
                people.add(person)
                batch.add(person)
            }
        }
        wait("Indexing not finished for $numberOfDocs docs in index $index") { kerch.search(index).request().count() == numberOfDocs.toLong() }
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
        wait("Index not finished") { kerch.search(index).get(id).isExists }
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

    data class Person(var name: String,
                      var age: Int,
                      var gender: Gender) : Document() {
        constructor(faker: Faker) : this(faker.name().fullName(), faker.number().numberBetween(20, 80), faker.options().option(Gender::class.java))
    }

    fun randomPojo(): SamplePojo {
        return SamplePojo(faker)
    }

    enum class Gender { MALE, FEMALE }
}
