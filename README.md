# kerch
An (opinionated) set of Kotlin utilities for ElasticSearch

Highlights:
* Everything in ES is a `Document`, has and `id` and a `version`
* A set of [extension functions](https://github.com/blueanvil/kerch/blob/master/src/main/kotlin/com/blueanvil/kerch/extensions.kt) will help with cleaner search code
* Not an ElasticSearch DSL (check out https://github.com/mbuhot/eskotlin for that)
* Some of the key components:
  * [Kerch](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch/-kerch/index.html)
  * [Search](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch.search/-search/index.html)
  * [Indexer](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch.index/-indexer/index.html)
  * [IndexWrapper](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch.index/-index-wrapper/index.html)
  * [Admin](https://blueanvil.github.io/kerch/etc/dokka/kerch/com.blueanvil.kerch/-admin/index.html)
  * Krude (see below)

## Dependency

```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.blueanvil:kerch:0.9.15'
}
```

## Standard flow
#### Component bootstrap
```
val kerch = Kerch("clustername", listOf("localhost:9300"))

// Create an index
kerch.index(indexName).create()
```
#### Index data
```
// Index a document
kerch.indexer(indexName).index(MyDocument(...))

// Batch index
kerch.indexer(indexName).batch<Person>().use { batch ->
    ...
    batch.add(MyDocument(...))
}
```
#### Search
(Some examples use https://github.com/mbuhot/eskotlin)
```
// Search with a query
kerch.search(indexName)
     .request()
     .setQuery(term { "gender" to "MALE" })
     .hits()
     .map { kerch.document(it, MyDocument::class) }
     .forEach { doc ->
         // do something with doc
     }

// Scroll (see https://www.elastic.co/guide/en/elasticsearch/reference/current/search-request-scroll.html)
kerch.search(indexName)
     .request()
     .scroll()
     .map { hit -> hit.id }
```

## The Krude module
The Krude module is a thin wrapper over the core Kerch/ElasticSearch functionality and provides a way to manage a more structured
data model, while helping you avoid mapping collisions when storing multiple object types in the same index.

Let's assume we have the following objects:
```
data class Person(var identifier: String): Document()
data class Disk  (var identifier: Long): Document()
```

Say we want to store objects of both types in the same index. In this case we'd face a collision when we'd want to map the ElasticSearch
field `identifier` if we want to store both objects in the same manner:
```
{"identifier": "xyz"}
{"identifier": 234}
```

A simple way to avoid this is to create an object for each type.
```
{"person": {"identifier": "xyz"}}
{"disk": {"identifier": 234}}
``` 

This would then allow us to have specialised mappings for each of these fields without any conflicts:
```
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
The Krude module implements the above JSON serialization mechanism for reading/writing ElasticSearch data. It offers a simple
wiring technique and minimal configuration:
```
@KrudeType(index = "dataobjects", type = "person")
data class Person(var identifier: String): KrudeObject()

...

// Packages to scan for sub-classes of KrudeObject
val packages = listOf("com...")

// Create a Krudes instance
val krudes = Krudes(kerch, packages)

// and then a Krude instance for reading/writing Person instances
val people = krudes.forType(Person::class)

people.save(Person(...))
people.find(term { people.field("identifier") to "xyz" })
      .forEach { person->
          ...
      }
```