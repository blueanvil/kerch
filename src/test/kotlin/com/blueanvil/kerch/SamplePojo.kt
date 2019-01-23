package com.blueanvil.kerch

import com.blueanvil.kerch.krude.ChildPojo
import com.blueanvil.kerch.krude.KrudeObject
import com.blueanvil.kerch.krude.KrudeType
import com.blueanvil.kerch.krude.SampleType
import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * @author Cosmin Marginean
 */
@KrudeType(index = "krude", type = "samplepojo")
class SamplePojo
@JsonCreator
constructor(@JsonProperty var name: String?,
            @JsonProperty var height: Int?,
            @JsonProperty var width: Int?,
            @JsonProperty var type: SampleType?
//            ,            @JsonProperty var child: ChildPojo?
) : KrudeObject() {
}