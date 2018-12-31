package com.blueanvil.kerch

import java.util.*

/**
 * @author Cosmin Marginean
 */
fun uuid(): String {
    return UUID.randomUUID().toString().toLowerCase().replace("-".toRegex(), "")
}
