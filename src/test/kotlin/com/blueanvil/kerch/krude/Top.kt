package com.blueanvil.kerch.krude

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type")
@JsonSubTypes(JsonSubTypes.Type(value = Bottom::class, name = "bottom"))
@KrudeType(index = "topbottom", type = "random")
abstract class Top : KrudeObject()