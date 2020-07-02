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
    compile 'com.github.blueanvil:kerch:1.0.30'
}
```

## Key concepts
* Kerch uses `ElasticsearchDocument` objects to read/write data to/from Elasticsearch.
* Indexing and searching is done through an `IndexStore` component
* An `Admin` component manages (at a low level) the index lifecycle, aliases and templates 
* The `Kerch` class is the core component which manages the Elasticsearch connection. It can create on the fly instances of `IndexStore` and `Admin` 
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

#### Search
_Note: Some examples use https://github.com/mbuhot/eskotlin_
```kotlin
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
```

## The Nestie module
The Nestie module is a thin wrapper over the core Kerch/ElasticSearch functionality and provides a way to manage complex
data models, and helps you avoid mapping collisions when storing multiple object types in the same index.

Let's assume we have the following objects:
```
data class Person(var identifier: String): ElasticsearchDocument()
data class Disk  (var identifier: Long): ElasticsearchDocument()
```

Say we want to store objects of both types in the same index. In this case we'd face a collision when we'd want to map the ElasticSearch
field `identifier` if we want to store both objects in the same manner:
```json
{"identifier": "xyz"}
{"identifier": 234}
```

A simple way to avoid this is to create an object for each type.
```json
{"person": {"identifier": "xyz"}}
{"disk": {"identifier": 234}}
``` 

This would then allow us to have specialised mappings for each of these fields without any conflicts:
```json
"mappings": {
  ...
    "properties": {
      "person": {
          "type": "object",
          "properties": {
              "identifier": {
                  "type": "text"
              }
          }
      },
      "disk": {
          "type": "object",
          "properties": {
              "identifier": {
                  "type": "integer"
              }
          }
      }
    }
}
```
The Nestie module implements the above JSON serialization mechanism for reading/writing ElasticSearch data. It offers a simple
wiring technique and minimal configuration:
```kotlin
@NestieDoc(index = "dataobjects", type = "person")
data class Person(var identifier: String): ElasticsearchDocument() 

...

val nestie = Nestie(nodes = listOf("localhost:9200"), packages = listOf("com.blueanvil"))
val store = nestie.store(MyDocument::class)

store.save(MyDocument())
val request = store.searchRequest().query(QueryBuilders.termQuery("tag", "blog"))
store.search(request)
        .forEach { doc ->
            // process doc
        }
```

Alternatively, the index name can be passed at runtime instead of setting it as an annotation parameter:
```kotlin
@NestieDoc(type = "person")
data class Person(var identifier: String): ElasticsearchDocument() 

...

val store = nestie.store(Person::class, "index_$indexName")
```

# License Information
The code is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
