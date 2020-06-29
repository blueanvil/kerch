# kerch
An (opinionated) set of Kotlin utilities for ElasticSearch

Highlights:
* Everything in ES is an `ElasticsearchDocument`, has and `id` and a `version`
* A set of [extension functions](https://github.com/blueanvil/kerch/blob/master/src/main/kotlin/com/blueanvil/kerch/extensions.kt) will help with cleaner search code
* Not an ElasticSearch DSL (check out https://github.com/mbuhot/eskotlin for that)
* Some of the key components:
  * [Kerch](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch/-kerch/index.html)
  * [IndexStore](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch/-index-store/index.html)
  * [IndexWrapper](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch/-index-wrapper/index.html)
  * [Admin](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch/-admin/index.html)
  * [Nestie](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch.nestie/-nestie/index.html) and [NestieIndexStore](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch.nestie/-nestie-index-store/index.html) (see below for details)

## Dependency

```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.blueanvil:kerch:1.0.30'
}
```

## Standard flow
#### Component bootstrap
```kotlin
// Create a Kerch instance and obtain a store reference
val kerch = Kerch(listOf("localhost:9200"))
val store = kerch.store(indexName)
```
#### Index data
```kotlin
// Index data
store.index(MyDocument())

store.indexRaw("id1", """{"name": "Walter" ...}""")

store.batch().use { batch ->
    batch.add("idx", """{"name": "..." ...}""")
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
