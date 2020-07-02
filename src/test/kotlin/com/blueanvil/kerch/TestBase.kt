package com.blueanvil.kerch

import com.blueanvil.kerch.nestie.Nestie
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.javafaker.Faker
import khttp.get
import org.apache.commons.io.IOUtils
import org.elasticsearch.index.query.QueryBuilders
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testng.Assert.assertEquals
import org.testng.annotations.*
import java.nio.charset.StandardCharsets

/**
 * @author Cosmin Marginean
 */
abstract class TestBase {

    companion object {
        private val log = LoggerFactory.getLogger(TestBase::class.java)

        val container = ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch-oss:7.6.2")
        lateinit var kerch: Kerch
        lateinit var nestie: Nestie
        val faker = Faker()
    }

    @BeforeSuite
    fun beforeTests() {
        container.start()
        kerch = Kerch(nodes = listOf(container.httpHostAddress), objectMapper = jacksonObjectMapper())
        nestie = Nestie(nodes = listOf(container.httpHostAddress), packages = listOf("com.blueanvil"))
    }

    @AfterSuite
    fun afterTests() {
        container.close()
    }

    fun indexPeople(index: String, numberOfDocs: Int = 100): List<Person> {
        var people: MutableList<Person> = ArrayList()
        kerch.store(index).docBatch<Person>().use { batch ->
            repeat(numberOfDocs) {
                val person = Person(faker)
                people.add(person)
                batch.add(person)
            }
        }
        wait("Indexing not finished for $numberOfDocs docs in index $index") { kerch.store(index).count() == numberOfDocs.toLong() }
        return people
    }

    fun peopleIndex(): String {
        val index = randomIndex("people.")
        createTemplate("template-people", index)
        return index
    }

    fun randomIndex(baseName: String = "randomIndex"): String {
        val index = "${baseName}.${uuid()}"
        log.info("Random index: $index")
        return index
    }

    fun count(index: String): Long {
        val get = get("http://${container.httpHostAddress}/${index}/_count")
        if (get.statusCode == 200) {
            return JSONObject(get.text).getLong("count")
        }
        return 0
    }

    fun waitToExist(index: String, id: String) {
        wait("Index not finished") { kerch.store(index).exists(id) }
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
        assertEquals(JSONObject(json1).toString(), JSONObject(json2).toString())
    }

    fun showCaseKerch() {
        val indexName = "myindex"
        val kerch = Kerch(listOf("localhost:9200"))
        val store = kerch.store(indexName)

        // Create index
        store.createIndex()

        // Index a custom object (`MyDocument` inherits from `ElasticsearchDocument`)
        store.index(MyDocument())

        // Index a raw JSON string
        store.indexRaw("id1", """{"name": "Walter" ...}""")

        // Batch indexing
        store.batch().use { batch ->
            batch.add("idx", """{"name": "..." ...}""")
            batch.add("idy", """{"name": "..." ...}""")
        }

        // Search
        val request = store.searchRequest().query(QueryBuilders.termQuery("tag", "blog"))
        store.search(request)
                .map { hit -> kerch.document(hit, MyDocument::class) }
                .forEach { doc ->
                    // process doc
                }

        // Scroll
        store.scroll(request)
                .forEach { hit ->
                    // process hit
                }
    }

    fun nestieConcept() {
        val nestie = Nestie(nodes = listOf("localhost:9200"), packages = listOf("com.blueanvil"))
        val store = nestie.store(MyDocument::class)

        store.save(MyDocument())
        val request = store.searchRequest().query(QueryBuilders.termQuery("tag", "blog"))
        store.search(request)
                .forEach { doc ->
                    // process doc
                }
    }

    class MyDocument : ElasticsearchDocument() {

    }
}
