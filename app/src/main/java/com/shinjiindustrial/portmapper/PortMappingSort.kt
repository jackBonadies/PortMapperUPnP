package com.shinjiindustrial.portmapper

 open class PortMapperComparatorBase(ascending : Boolean) : Comparator<PortMapping> {

     val orderBySwitch = if(ascending) 1 else -1

    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        var cmp = p1.ExternalPort.compareTo(p2.ExternalPort)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        cmp = p1.Protocol.compareTo(p2.Protocol)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        //maybe 100% step before here
        cmp = p1.InternalPort.compareTo(p2.InternalPort)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        cmp = p1.ActualExternalIP.compareTo(p2.ActualExternalIP)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        cmp = p1.Description.compareTo(p2.Description)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        cmp = p1.InternalIP.compareTo(p2.InternalIP)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        return 0
    }
}

open class PortMapperComparatorExternalPort(ascending : Boolean) : PortMapperComparatorBase(ascending) {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.ExternalPort.compareTo(p2.ExternalPort)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        return super.compare(p1, p2)
    }
}

open class PortMapperComparatorInternalPort(ascending : Boolean) : PortMapperComparatorBase(ascending) {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.InternalPort.compareTo(p2.InternalPort)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
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


open class PortMapperComparatorDevice(ascending : Boolean) : PortMapperComparatorBase(ascending) {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.InternalIP.compareTo(p2.InternalIP)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        return super.compare(p1, p2)
    }
}

open class PortMapperComparatorExpiration(ascending : Boolean) : PortMapperComparatorBase(ascending) {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.LeaseDuration.compareTo(p2.LeaseDuration)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        return super.compare(p1, p2)
    }
}

open class PortMapperComparatorDescription(ascending : Boolean) : PortMapperComparatorBase(ascending) {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.Description.compareTo(p2.Description)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        return super.compare(p1, p2)
    }
}

open class PortMapperComparerSlot(ascending : Boolean) : PortMapperComparatorBase(ascending) {
    override fun compare(p1: PortMapping, p2: PortMapping): Int {
        val cmp = p1.Slot.compareTo(p2.Slot)
        if(cmp != 0)
        {
            return orderBySwitch * cmp
        }
        return super.compare(p1, p2)
    }
}