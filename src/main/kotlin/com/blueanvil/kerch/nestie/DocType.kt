package com.blueanvil.kerch.nestie

import java.lang.annotation.Inherited


/**
 * @author Cosmin Marginean
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class DocType(val index: String,
                         val type: String)
