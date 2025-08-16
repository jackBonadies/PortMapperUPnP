package com.shinjiindustrial.portmapper.domain


class PortMapping(
    description: String,
    remoteHost: String,
    localIP: String,
    externalPort: Int,
    internalPort: Int,
    protocol: String,
    enabled: Boolean,
    leaseDuration: Int,
    actionExternalIP : String,
    timeReadLeaseDurationMs : Long,
    pseudoSlot : Int)
{
    // the returned ip from get port mapping
    // https://upnp.org/specs/gw/UPnP-gw-WANIPConnection-v2-Service.pdf page 17
    // (this is an optional filter for another device on the network that is trying to reach you.)
    // "The NAT Traversal or port mapping functionality allows creation of mappings for both TCP and UDP
    //protocols between an external IGD port (called ExternalPort) and an internal client address associated with
    //one of its ports (respectively called InternalClient and InternalPort). It is also possible to narrow the
    //mapping by limiting the mapping to a specific remote host1"
    // (this means that the external ip is a given, it is the igd device)
    // "2.3.17 RemoteHost
    //This variable represents the source of inbound IP packets. This variable can contain a host name or a
    //standard IPv4 address representation. This state variable MUST be formatted as:
    //• a domain name of a network host like it is defined in [RFC 1035],
    //• or as a set of four decimal digit groups separated by "." as defined in [RFC 3986],
    //• or an empty string.
    //This will be a wildcard in most cases (an empty string). In version 2.0, NAT vendors are REQUIRED to
    //support non-wildcarded IP addresses in addition to wildcards. A non-wildcard value will allow for “narrow”
    //port mappings, which MAY be desirable in some usage scenarios. When RemoteHost is a wildcard, all
    //traffic sent to the ExternalPort on the WAN interface of the gateway is forwarded to the InternalClient on
    //the InternalPort (this corresponds to the endpoint independent filtering behaviour defined in the [RFC
    //4787]). When RemoteHost is specified as a specific external IP address as opposed to a wildcard, the NAT
    //will only forward inbound packets from this RemoteHost to the InternalClient. All other packets will
    //dropped (this corresponds to the address dependent filtering behaviour defined in [RFC 4787])."
    var RemoteHost : String = remoteHost
    // the actual ip of the IGD device
    var ActualExternalIP : String = actionExternalIP
    var InternalIP : String = localIP
    var ExternalPort : Int = externalPort
    var InternalPort : Int= internalPort
    var Protocol : String = protocol
    var Enabled : Boolean = enabled
    var LeaseDuration : Int  = leaseDuration
    var Description : String = description

    var TimeReadLeaseDurationMs : Long = timeReadLeaseDurationMs
    var Slot : Int = pseudoSlot

    fun getKey() : Pair<Int, String>
    {
        return Pair<Int, String>(this.ExternalPort, this.Protocol)
    }

    fun shortName() : String
    {
        return formatShortName(Protocol,ActualExternalIP,ExternalPort.toString())
    }

    fun getRemainingLeaseTime() : Int
    {
        val secondsPassed = (System.currentTimeMillis() - TimeReadLeaseDurationMs)/1000L
        val timeToExpiration = (LeaseDuration.toLong() - secondsPassed)
        return timeToExpiration.toInt()
    }

    fun getRemainingLeaseTimeString() : String
    {
        // show only 2 units (i.e. days and hours. or hours and minutes. or minutes and seconds. or just seconds)
        val totalSecs = getRemainingLeaseTime()

        val dhms = getDHMS(totalSecs)

        val hasDays = dhms.days >= 1
        val hasHours = dhms.hours >= 1
        val hasMinutes = dhms.mins >= 1
        val hasSeconds = dhms.seconds >= 1

        if (hasDays)
        {
            return "${dhms.days} day${_plural(dhms.days)}, ${dhms.hours} hour${_plural(dhms.hours)}"
        }
        else if(hasHours)
        {
            return "${dhms.hours} hour${_plural(dhms.hours)}, ${dhms.mins} minute${_plural(dhms.mins)}"
        }
        else if(hasMinutes)
        {
            return "${dhms.mins} minute${_plural(dhms.mins)}, ${dhms.seconds} second${_plural(dhms.seconds)}"
        }
        else if(hasSeconds)
        {
            return "${dhms.seconds} second${_plural(dhms.seconds)}"
        }
        else
        {
            return "Expired"
        }
    }

    fun _plural(value : Int) : String
    {
        return if (value > 1) "s" else ""
    }

    fun toStringFull() : String
    {
        return "PortMapping(RemoteHost=$RemoteHost, ActualExternalIP=$ActualExternalIP, InternalIP=$InternalIP, ExternalPort=$ExternalPort, InternalPort=$InternalPort, Protocol=$Protocol, Enabled=$Enabled, LeaseDuration=$LeaseDuration, Description=$Description, TimeReadLeaseDurationMs=$TimeReadLeaseDurationMs, Slot=$Slot)"
    }
}

data class DayHourMinSec(val days : Int, val hours : Int, val mins : Int, val seconds : Int)
{
    fun totalSeconds() : Int
    {
        return days * 3600 * 24 + hours * 3600 + mins * 60 + seconds
    }
}

fun getDHMS(totalSeconds : Int) : DayHourMinSec
{
    val days = totalSeconds / (24*3600)
    val hours = (totalSeconds % (24*3600)) / 3600
    val mins =  (totalSeconds % (3600)) / 60
    val secs = (totalSeconds % (60))
    return DayHourMinSec(days, hours, mins, secs)
}

fun formatShortName(protocol: String, externalIp: String, externalPort: String) : String
{
    return "$protocol rule at $externalIp:$externalPort"
}