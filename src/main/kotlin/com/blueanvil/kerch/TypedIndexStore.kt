package com.blueanvil.kerch

import com.blueanvil.kerch.batch.DocumentBatch
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
open class TypedIndexStore<T : Document>(kerch: Kerch,
                                         index: String,
                                         protected val docType: KClass<T>) : IndexStore(kerch, index) {

    fun get(id: String): T? {
        return get(id, docType)
    }

    fun save(doc: T, waitRefresh: Boolean = true): String {
        return index(doc, waitRefresh)
    }

    fun docBatch(size: Int = 100,
                 afterIndex: ((Collection<T>) -> Unit)? = null): DocumentBatch<T> {
        return DocumentBatch(this, size, afterIndex)
    }
}