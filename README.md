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

## Dependency

```
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.blueanvil:kerch:0.9.5'
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
