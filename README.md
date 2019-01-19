# kerch
An (opinionated) set of Kotlin wrappers and utilities for ElasticSearch

Some of the principles:
* Everything in ES is a `Document` (has and `id` and a `version`)
* A set of extension functions help with cleaner search

## Dependency

```
TODO
```

### Component bootstrap
```
val kerch = Kerch("clustername", listOf("localhost:9300"))

// Create an index
kerch.index(indexName).create()
```

### Index data
```
// Index a document
kerch.indexer(indexName).index(MyDocument(...))

// Batch index
kerch.indexer(indexName).batch<Person>().use { batch ->
    ...
    batch.add(MyDocument(...))
}
```

### Search
```
kerch.search(indexName)
```
