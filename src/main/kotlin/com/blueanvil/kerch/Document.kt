package com.blueanvil.kerch

/**
 * @author Cosmin Marginean
 */
abstract class Document(var id: String = uuid(),
                        var version: Long = 0)