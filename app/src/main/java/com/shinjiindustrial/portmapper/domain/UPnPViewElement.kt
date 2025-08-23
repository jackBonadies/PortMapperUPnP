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
        return UnderlyingElement is IIGDDevice
    }

    fun GetUnderlyingIGDDevice() : IIGDDevice
    {
        return UnderlyingElement as IIGDDevice
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
