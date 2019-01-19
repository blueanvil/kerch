# kerch
An (opinionated) set of Kotlin utilities for ElasticSearch

Some of the principles:
* Everything in ES is a `Document`, has and `id` and a `version`
* A set of [extension functions](https://github.com/blueanvil/kerch/blob/master/src/main/kotlin/com/blueanvil/kerch/extensions.kt) will help with cleaner search code
* Not an ElasticSearch DSL (check out https://github.com/mbuhot/eskotlin for that)

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
##### Component bootstrap
```
val kerch = Kerch("clustername", listOf("localhost:9300"))

// Create an index
kerch.index(indexName).create()
```
##### Index data
```
// Index a document
kerch.indexer(indexName).index(MyDocument(...))

// Batch index
kerch.indexer(indexName).batch<Person>().use { batch ->
    ...
    batch.add(MyDocument(...))
}
```
##### Search
(Some examples also use https://github.com/mbuhot/eskotlin)
```
kerch.search(indexName)
     .request()
     .setQuery(term { "gender" to "MALE" }).count()
```
