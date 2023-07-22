package com.shinjiindustrial.portmapper

 open class PortMapperComparatorBase : Comparator<PortMapping> {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        var cmp = p1.ExternalPort.compareTo(p2.ExternalPort)
        if(cmp != 0)
        {
            return cmp
        }
        cmp = p1.Protocol.compareTo(p2.Protocol)
        if(cmp != 0)
        {
            return cmp
        }
        //maybe 100% step before here
        cmp = p1.InternalPort.compareTo(p2.InternalPort)
        if(cmp != 0)
        {
            return cmp
        }
        cmp = p1.ExternalIP.compareTo(p2.ExternalIP)
        if(cmp != 0)
        {
            return cmp
        }
        cmp = p1.Description.compareTo(p2.Description)
        if(cmp != 0)
        {
            return cmp
        }
        cmp = p1.InternalIP.compareTo(p2.InternalIP)
        if(cmp != 0)
        {
            return cmp
        }
        return 0
    }
}

open class PortMapperComparatorExternalPort : PortMapperComparatorBase() {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.ExternalPort.compareTo(p2.ExternalPort)
        if(cmp != 0)
        {
            return cmp
        }
        return super.compare(p1, p2)
    }
}

open class PortMapperComparatorInternalPort : PortMapperComparatorBase() {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.InternalPort.compareTo(p2.InternalPort)
        if(cmp != 0)
        {
            return cmp
        }
        return super.compare(p1, p2)
    }
}


//        open class PortMapperComparatorIndex : PortMapperComparatorBase() {
//            override fun compare(p1: PortMapping, p2: PortMapping): Int {
//                val cmp = p1.InternalPort.compareTo(p2.InternalPort)
//                if(cmp != 0)
//                {
//                    return cmp
//                }
//                return super.compare(p1, p2)
//            }
//        }


open class PortMapperComparatorDevice : PortMapperComparatorBase() {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.InternalIP.compareTo(p2.InternalIP)
        if(cmp != 0)
        {
            return cmp
        }
        return super.compare(p1, p2)
    }
}

open class PortMapperComparatorExpiration : PortMapperComparatorBase() {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.LeaseDuration.compareTo(p2.LeaseDuration)
        if(cmp != 0)
        {
            return cmp
        }
        return super.compare(p1, p2)
    }
}

open class PortMapperComparatorDescription : PortMapperComparatorBase() {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.Description.compareTo(p2.Description)
        if(cmp != 0)
        {
            return cmp
        }
        return super.compare(p1, p2)
    }
}