package com.blueanvil.kerch.nestie

import java.lang.annotation.Inherited


/**
 * @author Cosmin Marginean
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class NestieDoc(val index: String = NO_NESTIE_DOC_INDEX,
                           val type: String)

const val NO_NESTIE_DOC_INDEX = "NO_NESTIE_DOC_INDEX"
