package com.blueanvil.kerch.nestie

import com.blueanvil.kerch.TestBase
import com.blueanvil.kerch.uuid
import org.testng.Assert.assertEquals
import org.testng.annotations.Test

/**
 * @author Cosmin Marginean
 */
class MultipleObjectsInIndex : TestBase() {

    @Test
    fun multipleObjectsInSameIndex() {
        val index = "multi-objects-same-index"
        val users = nestie.store(MOUser::class, index)
        users.createIndex()

        users.save(MOUser("name1"), true)
        users.save(MOUser("name2"), true)

        val blogs = nestie.store(MOBlog::class, index)
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

@NestieDoc(type = "mouser")
data class MOUser(val name: String,
                  val id: String = uuid())

@NestieDoc(type = "moblog-entry")
data class MOBlog(val title: String,
                  val id: String = uuid())