package com.blueanvil.kerch.krude

import java.lang.annotation.Inherited


/**
 * @author Cosmin Marginean
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
annotation class KrudeType(val index: String,
                           val type: String)
