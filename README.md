# Kerch
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.com/blueanvil/kerch.svg?branch=master)](https://travis-ci.com/blueanvil/kerch)
[![Coverage Status](https://coveralls.io/repos/github/blueanvil/kerch/badge.svg?branch=master)](https://coveralls.io/github/blueanvil/kerch?branch=master)

Kerch is an (opinionated) set of Kotlin utilities for Elasticsearch 7.x. The [0.9.x](https://github.com/blueanvil/kerch/tree/0.9.x) branch is an older version compatible with Elasticsearch 6.x and still maintained.

# Gradle

```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.blueanvil:kerch:1.0.33'
}
```

## Key concepts
* Kerch uses `ElasticsearchDocument` objects to read/write data to/from Elasticsearch.
* Indexing and searching is done through an `IndexStore` component.
* An `Admin` component manages indices, aliases and templates.
* The `Kerch` class is the core component which manages the Elasticsearch connection. It creates on demands instances of `IndexStore` and `Admin` 
* `Nestie` is an extension component for storing objects of multiple types in the same index transparently 

## Kerch - Indexing data
```kotlin
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
```

## Kerch - Search & scroll
```kotlin
// Search
val request = store.searchRequest()
        .query(termQuery("tag", "blog"))
        .paging(0, 15)
        .sort("name")
val docs: List<MyDocument> = store.search(request, MyDocument::class)

// Scroll
store.scroll(termQuery("tag", "blog"))
        .forEach { hit ->
            // process hit
        }
```

# The Nestie module
The Nestie module is a thin wrapper over the core Kerch functionality and provides a way to manage complex
data models. Crucially, it helps you avoid mapping conflicts when storing multiple object types in the same index.

### Problem description
Let's assume we have the following objects:
```kotlin
data class Person(var identifier: String): ElasticsearchDocument()
data class Disk  (var identifier: Long): ElasticsearchDocument()
```

Assuming we want to store objects of both types in the same index, we'd face a mapping conflict when we'd want to map the ElasticSearch
field `identifier`:
```json
{"identifier": "xyz"}
{"identifier": 234}
```
### Solution
Nestie solves this by creating a wrapper object for each type:
```json
{"person": {"identifier": "xyz"} }
{"disk":   {"identifier": 234} }
``` 

This would then allow us to have specialised mappings for each of these fields without any conflicts:
```json
"mappings": {
    "properties": {
      "person": {
          "type": "object",
          "properties": {
              "identifier": {"type": "text"}
          }
      },
      
      "disk": {
          "type": "object",
          "properties": {
              "identifier": {"type": "integer"}
          }
```

### Working with Nestie
Nestie provides an equivalent of Kerch's `IndexStore`, called `NestieIndexStore`. This provides similar capabilities, with the additional handling
of JSON serialization/deserialization based on the object wrapping technique above. 

Nestie objects are annoted with `@NestieDoc`
```kotlin
@NestieDoc(type = "person")
data class Person(val name: String,
                  val gender: Gender) : ElasticsearchDocument()
```
This essentially instructs Nestie to wrap and serialize the object as follows before writing it to Elasticsearch:
```json
{"person": {"name": "..."} }
```

Indexing and searching with a `NestieIndexStore` is very similar to working with an `IndexStore`:
```kotlin
val nestie = Nestie(nodes = listOf("localhost:9200"), packages = listOf("com.blueanvil"))
val store = nestie.store(docType = Person::class, index = "dataobjects")

// Index data
store.save(Person("John Smith", Gender.MALE))

// Search
val request = store.searchRequest()
        .query(matchQuery(Nestie.field(Person::class, "name"), "john"))
        .paging(0, 15)
        .sort("name.keyword")
store.search(request)
        .forEach { person ->
            println(person.name)
        }
```

# License Information
The code is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
