package com.shinjiindustrial.portmapper.domain

class UPnPViewElement constructor(
    underlyingElement : Any,
    isSpecialEmpty : Boolean = false)
{
    var UnderlyingElement : Any
    var IsSpecialEmpty : Boolean

    init
    {
        IsSpecialEmpty = isSpecialEmpty
        UnderlyingElement = underlyingElement
    }

    fun IsIGDDevice() : Boolean
    {
        return UnderlyingElement is IGDDevice
    }

    fun GetUnderlyingIGDDevice() : IGDDevice
    {
        return UnderlyingElement as IGDDevice
    }

    fun IsPortMapping() : Boolean
    {
        return UnderlyingElement is PortMapping
    }

    fun GetUnderlyingPortMapping() : PortMapping
    {
        return UnderlyingElement as PortMapping
    }
}
