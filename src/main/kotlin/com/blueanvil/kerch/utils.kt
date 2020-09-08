package com.blueanvil.kerch

import com.blueanvil.kerch.nestie.Nestie
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.memberProperties

/**
 * @author Cosmin Marginean
 */
fun KClass<*>.nestieField(fieldName: String): String {
    return "${Nestie.annotation(this).type}.$fieldName"
}

internal fun uuid(): String {
    return UUID.randomUUID().toString().toLowerCase().replace("-", "")
}

internal fun <T : Annotation> annotation(cls: KClass<*>, annotationClass: KClass<T>): T? {
    val annotation = cls.annotations.find { it.annotationClass == annotationClass }
    if (annotation != null) {
        return annotation as T
    }

    cls.allSuperclasses.forEach {
        val parentAnnotation = it.annotations.find { a -> a.annotationClass == annotationClass }
        if (parentAnnotation != null) {
            return parentAnnotation as T
        }
    }
    return null
}

internal val Any.documentId: String
    get() {
        if (this is ElasticsearchDocument) {
            return this.id
        }

        val idProperty = this.javaClass.kotlin.memberProperties.find { it.name == "id" }
                ?: throw IllegalStateException("Class ${this::class.qualifiedName} doesn't have an 'id' property")
        return idProperty.get(this) as String
    }

internal var Any.version: Long
    get() {
        if (this is ElasticsearchDocument) {
            return this.version
        }

        val versionProp = this.javaClass.kotlin.memberProperties.find { it.name == "version" } ?: return 0
        return versionProp.get(this) as Long
    }
    set(value) {
        if (this is ElasticsearchDocument) {
            this.version = value
        } else {
            val versionProp = this.javaClass.kotlin.memberProperties.find { it.name == "version" }
            if (versionProp != null && versionProp is KMutableProperty<*>) {
                versionProp.setter.call(this, value)
            }
        }
    }


object KerchConst {
    const val DEFAULTTYPE = "defaulttype"
}