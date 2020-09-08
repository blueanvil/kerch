# Kerch
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build Status](https://travis-ci.com/blueanvil/kerch.svg?branch=master)](https://travis-ci.com/blueanvil/kerch)
[![Coverage Status](https://coveralls.io/repos/github/blueanvil/kerch/badge.svg?branch=master)](https://coveralls.io/github/blueanvil/kerch?branch=master)

Kerch is an (opinionated) set of Kotlin utilities for Elasticsearch 7.x.

The [0.9.x](https://github.com/blueanvil/kerch/tree/0.9.x) branch is an older version compatible with Elasticsearch 6.x and still being maintained.

# Gradle

```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.blueanvil:kerch:1.1.0'
}
```

## Key concepts
* All objects stored in Elasticsearch must have an `id` property, and optionally a `seqNo`. Alternatively (and recommended) they can inherit from `ElasticsearchDocument`.
* Use `IndexStore` for index and search.
* Use `Admin` for managing indices, aliases, templates.
* Use `Kerch` component to manage the Elasticsearch connection and to create `IndexStore` and `Admin` objects 
* `Nestie` is an extension for storing objects of different types in the same index 

## Kerch - Indexing data
```kotlin
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
```

## Kerch - Search & scroll
```kotlin
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
```

# The Nestie module
The Nestie module is a thin wrapper over the core Kerch functionality and provides a way
to manage more complex data models. Crucially, it helps you avoid mapping conflicts when
storing object of different types in the same index.

### Problem description
Let's assume we have the following objects:
```kotlin
data class Person(var identification: IdDocument)
data class Disk  (var identification: Long)
```

Assuming we want to store objects of both types in the same index, we'd face a mapping
conflict when configuring the field `identification`:
```json
{ "identification": { "type": "passport" ...} }
{ "identification": 234 }
```
### Solution
Nestie solves this by creating a wrapper object for each type:
```json
{"person": { "identification": { "type": "passport" ...} } }
{"disk":   { "identification": 234 } }
``` 

This would then allow us to have specialised mappings for each of these fields without any conflicts:
```json
"mappings": {
    "properties": {
      "person": {
          "type": "object",
          "properties": {
              "identification": { "type": "object" ... }
          }
      },
      
      "disk": {
          "type": "object",
          "properties": {
              "identification": { "type": "long" }
          }
```

### Working with Nestie
Nestie provides an equivalent to Kerch's `IndexStore`, called `NestieIndexStore`.
This provides similar capabilities, with the additional handling of JSON serialization/deserialization,
based on the object wrapping technique above. 

Nestie objects are annoted with `@NestieDoc`
```kotlin
@NestieDoc(type = "person")
data class Person(val name: String,
                  val gender: Gender) : ElasticsearchDocument()
```
This instructs Nestie to wrap and serialize the object to Elasticsearch like this:
```json
{ "person": {"name": "..."} }
```

Indexing and searching with a `NestieIndexStore` is very similar to working with an `IndexStore`:
```kotlin
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
```

# License Information
The code is licensed under [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0).
