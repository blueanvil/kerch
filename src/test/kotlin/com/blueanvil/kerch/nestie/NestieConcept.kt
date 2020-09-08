package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.*
import org.elasticsearch.index.query.QueryBuilders

/**
 * @author Cosmin Marginean
 */
class NestieConcept : TestBase() {

    fun nestieConcept() {

        @NestieDoc(type = "person")
        data class Person(val name: String,
                          val gender: Gender) : ElasticsearchDocument()

        val nestie = Nestie(nodes = listOf("localhost:9200"), packages = listOf("com.blueanvil"))
        val store = nestie.store(docType = Person::class, index = "dataobjects")

        // Index data
        store.save(Person("John Smith", Gender.MALE))

        // Search
        val request = store.searchRequest()
                .query(QueryBuilders.matchQuery(Nestie.field(Person::class, "name"), "john"))
                .paging(0, 15)
                .sort("name.keyword")
        store.search(request)
                .forEach { person ->
                    println(person.name)
                }
    }

}