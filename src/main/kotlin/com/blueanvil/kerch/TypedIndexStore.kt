package com.blueanvil.kerch

import com.blueanvil.kerch.nestie.DocType
import com.blueanvil.kerch.nestie.Nestie
import kotlin.reflect.KClass

/**
 * @author Cosmin Marginean
 */
class TypedIndexStore<T : Document>(kerch: Kerch,
                                    index: String,
                                    private val docType: KClass<T>) : IndexStore(kerch, index) {

    private val annotation: DocType = Nestie.annotation(docType)

    fun get(id: String): T? {
        return get(id, docType)
    }

    fun save(doc: T, waitRefresh: Boolean = true): String {
        return index(doc, waitRefresh)
    }

    fun field(fieldName: String): String {
        return "${annotation.type}.$fieldName"
    }
}