package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.ElasticsearchDocument
import com.blueanvil.kerch.TestBase
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * @author Cosmin Marginean
 */
class MultipleObjectsInIndex : TestBase() {

    @Test
    fun multipleObjectsInSameIndex() {
        val users = nestie.store(MOUser::class)
        users.createIndex()

        users.save(MOUser("name1"), true)
        users.save(MOUser("name2"), true)

        val blogs = nestie.store(MOBlog::class)
        blogs.save(MOBlog("title 1"), true)
        blogs.save(MOBlog("title 2"), true)
        blogs.save(MOBlog("title 3"), true)

        assertEquals(2, users.search(users.searchRequest()).size)
        assertEquals(2, users.count())
        assertEquals(2, users.allIds().count())

        assertEquals(3, blogs.search(blogs.searchRequest()).size)
        assertEquals(3, blogs.count())
        assertEquals(3, blogs.allIds().count())
    }
}

@NestieDoc(index = "multi-objects-same-index", type = "mouser")
data class MOUser(val name: String) : ElasticsearchDocument()

@NestieDoc(index = "multi-objects-same-index", type = "moblog-entry")
data class MOBlog(val title: String) : ElasticsearchDocument()