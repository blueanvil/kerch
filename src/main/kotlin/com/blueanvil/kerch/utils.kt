package com.blueanvil.kerch

import com.blueanvil.kerch.nestie.Nestie
import org.reflections.Reflections
import org.reflections.scanners.SubTypesScanner
import org.reflections.scanners.TypeAnnotationsScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
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

fun uuid(): String {
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

internal fun reflections(packages: Collection<String>): Reflections {
    val config = ConfigurationBuilder()
    packages.forEach {
        config.addUrls(ClasspathHelper.forPackage(it))
    }

    config.setScanners(
            TypeAnnotationsScanner(),
            SubTypesScanner()
    )
    Reflections.log = null
    return Reflections(config)
}

internal fun documentId(document: Any): String {
    val idProperty = document.javaClass.kotlin.memberProperties.find { it.name == "id" }
            ?: throw IllegalStateException("Class ${document::class.qualifiedName} doesn't have an 'id' property")
    return idProperty.get(document) as String
}

internal fun sequenceNumber(document: Any): Long {
    val seqNoProperty = document.javaClass.kotlin.memberProperties.find { it.name == "seqNo" } ?: return 0
    return seqNoProperty.get(document) as Long
}

internal fun setSequenceNumber(document: Any, seqNo: Long) {
    val seqNoProperty = document.javaClass.kotlin.memberProperties.find { it.name == "seqNo" }
    if (seqNoProperty != null && seqNoProperty is KMutableProperty<*>) {
        seqNoProperty.setter.call(document, seqNo)
    }
}
