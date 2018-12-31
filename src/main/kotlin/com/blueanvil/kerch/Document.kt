package com.blueanvil.kerch

/**
 * @author Cosmin Marginean
 */
abstract class Document(val id: String? = uuid(),
                        var version: Long = 0)