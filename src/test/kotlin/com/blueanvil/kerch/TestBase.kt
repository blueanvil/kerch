package com.blueanvil.kerch

import com.blueanvil.kerch.nestie.Nestie
import com.blueanvil.kerch.nestie.NestieIndexStore
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.javafaker.Faker
import org.elasticsearch.index.query.QueryBuilders.termQuery
import org.json.JSONObject
import org.testcontainers.elasticsearch.ElasticsearchContainer
import org.testng.Assert.assertEquals
import org.testng.annotations.AfterSuite
import org.testng.annotations.BeforeSuite
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
abstract class TestBase {

    companion object {
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

    fun store(): IndexStore {
        val index = uuid()
        val jsonContent = resourceAsString("template.json")
                .replace("APPLIESTO", index)
        kerch.admin.createTemplate(uuid(), jsonContent)
        return kerch.store(index)
    }

    fun <T : Any> nestieStore(cls: KClass<T>): NestieIndexStore<T> {
        val index = uuid()
        val jsonContent = resourceAsString("template.json")
                .replace("APPLIESTO", index)
        kerch.admin.createTemplate(uuid(), jsonContent)
        return nestie.store(cls, index)
    }

    fun <T : Any> batchIndex(store: IndexStore, numberOfDocs: Int, newDoc: () -> T): List<T> {
        val docs = mutableListOf<T>()
        store.batch().use { batch ->
            repeat(numberOfDocs) {
                val doc = newDoc()
                batch.add(doc.documentId, kerch.toJson(doc))
                docs.add(doc)
            }
        }
        wait("Indexing not finished for $numberOfDocs docs in index ${store.indexName}") {
            store.count() == numberOfDocs.toLong()
        }
        return docs
    }

    fun <T : Any> batchIndex(nestieStore: NestieIndexStore<T>, numberOfDocs: Int, newDoc: () -> T): List<T> {
        val docs = mutableListOf<T>()
        nestieStore.docBatch().use { batch ->
            repeat(numberOfDocs) {
                val doc = newDoc()
                batch.add(doc)
                docs.add(doc)
            }
        }
        wait("Indexing not finished for $numberOfDocs docs in index ${nestieStore.indexName}") {
            nestieStore.count() == numberOfDocs.toLong()
        }
        return docs
    }

    fun waitToExist(store: IndexStore, id: String) {
        wait("Index not finished") { store.exists(id) }
    }

    fun waitToExist(store: NestieIndexStore<*>, id: String) {
        wait("Index not finished") { store.exists(id) }
    }

    fun assertSameJson(json1: String, json2: String) {
        assertEquals(JSONObject(json1).toString(), JSONObject(json2).toString())
    }

    fun showCaseKerch() {
        val indexName = "myindex"
        val kerch = Kerch(listOf("localhost:9200"))
        val store = kerch.store(indexName)

        // Create index
        store.createIndex()

        // Index a custom object
        store.index(MyDocument())

        // Index a raw JSON string
        store.indexRaw("id1", """{"name": "Walter" ...}""")

        // Batch indexing
        store.docBatch<Person>().use { batch ->
            batch.add(Person())
            batch.add(Person())
        }

        // Search
        val request = store.searchRequest()
                .query(termQuery("tag", "blog"))
                .paging(0, 15)
                .sort("name.keyword")
        val docs: List<MyDocument> = store.search(request, MyDocument::class)

        // Scroll
        store.scroll(Person::class, termQuery("gender", "MALE"))
                .forEach { person ->
                    // process record
                }
    }

    class MyDocument
}
