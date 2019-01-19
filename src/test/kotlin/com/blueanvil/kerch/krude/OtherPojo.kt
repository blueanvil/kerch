package com.blueanvil.kerch.krude

/**
 * @author Cosmin Marginean
 */
@KrudeType(index = "krude", type = "otherpojo")
data class OtherPojo(var name: String) : KrudeObject()