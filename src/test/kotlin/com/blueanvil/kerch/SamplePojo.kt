package com.blueanvil.kerch

import com.blueanvil.kerch.krude.ChildPojo
import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType
import com.blueanvil.kerch.krude.SampleType

/**
 * @author Cosmin Marginean
 */
@KrudeType(index = "krude", type = "samplepojo")
data class SamplePojo(var name: String,
                      var height: Int,
                      var width: Int,
                      var type: SampleType,
                      var child: ChildPojo) : KrudeObject() {
}