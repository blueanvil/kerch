package com.blueanvil.kerch

import org.elasticsearch.action.search.SearchRequest
import org.elasticsearch.search.builder.SearchSourceBuilder
import org.reflections.Reflections
import org.reflections.scanners.MethodAnnotationsScanner
import org.reflections.scanners.ResourcesScanner
import org.reflections.scanners.SubTypesScanner
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses

/**
 * @author Cosmin Marginean
 */
fun uuid(): String {
    return UUID.randomUUID().toString().toLowerCase().replace("-".toRegex(), "")
}

fun <T : Annotation> annotation(cls: KClass<*>, annotationClass: KClass<T>): T? {
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

fun reflections(packages: Collection<String>): Reflections {
    val config = ConfigurationBuilder()
    packages.forEach {
        config.addUrls(ClasspathHelper.forPackage(it))
    }

    config.setScanners(ResourcesScanner(),
            MethodAnnotationsScanner(),
            SubTypesScanner())
    Reflections.log = null
    return Reflections(config)
}
