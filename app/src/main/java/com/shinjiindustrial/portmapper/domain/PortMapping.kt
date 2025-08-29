package com.shinjiindustrial.portmapper.domain

import android.os.Parcelable
import android.os.SystemClock
import com.shinjiindustrial.portmapper.PortForwardApplication
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity
import kotlinx.parcelize.Parcelize

data class PortMappingWithPref(val portMapping: PortMapping, val portMappingPref: PortMappingPref? = null)
{
    fun getKey() : PortMappingKey
    {
        return portMapping.getKey()
    }

    fun getAutoRenewOrDefault() : Boolean
    {
        return portMappingPref?.autoRenew ?: false
    }

    fun getDesiredLeaseDurationOrDefault() : Int
    {
        return portMappingPref?.desiredLeaseDuration ?: portMapping.LeaseDuration
    }
}

data class PortMappingPref(val autoRenew : Boolean, val desiredLeaseDuration: Int) {

}

@Parcelize
data class PortMappingKey(val deviceIP: String, val externalPort: Int, val protocol: String) :
    Parcelable

// this is what is on the router
// with additional field for timeRead and psuedoSlot
data class PortMapping(
    val Description : String,
    val RemoteHost : String,
    val InternalIP : String,
    val ExternalPort : Int,
    val InternalPort : Int,
    val Protocol : String,
    val Enabled : Boolean,
    val LeaseDuration : Int,
    val DeviceIP : String,
    val TimeReadLeaseDurationMs : Long,
    val Slot : Int)
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


    // needs to be * for Verizon Router - CR1000B, but only for delete call
    // else one will get InvalidArgs exception
    fun getRemoteHostNormalizedForDelete(): String =
        this.RemoteHost.ifBlank { "*" }

    fun getKey() : PortMappingKey
    {
        return PortMappingKey(this.DeviceIP, this.ExternalPort, this.Protocol)
    }

    fun shortName() : String
    {
        return formatShortName(Protocol,DeviceIP,ExternalPort.toString())
    }

    fun getRemainingLeaseTime(now : Long = -1) : Int
    {
        val secondsPassed = ((if(now == -1L) SystemClock.elapsedRealtime() else now) - TimeReadLeaseDurationMs)/1000L
        val timeToExpiration = (LeaseDuration.toLong() - secondsPassed)
        return timeToExpiration.toInt()
    }

    fun getExpiresTimeMillis() : Long {
        return TimeReadLeaseDurationMs + LeaseDuration.toLong() * 1000;
    }

    fun getRemainingLeaseTimeRoughString(autoRenew : Boolean, now: Long = -1) : String
    {
        if (this.LeaseDuration == 0)
        {
            return "Expires Never"
        }

        val totalSecs = getRemainingLeaseTime(now)
        val dhms = getDHMS(totalSecs)

        val renewsExpiresString = if(autoRenew) "Renews in" else "Expires in"
        val renewStringTime = roundOneUnit(dhms)
        if (totalSecs < 60)
        {
            if (autoRenew) {
                if (totalSecs < PortForwardApplication.RENEW_RULE_WITHIN_X_SECONDS_OF_EXPIRING + 10)
                {
                    return "Renewing now"
                }
                else
                {
                    return "Renewing <1 minute"
                }
            }
            else {
                return "$renewsExpiresString <1 minute"
            }
        }
        else
        {
            return "$renewsExpiresString $renewStringTime"
        }

    }

    fun getRemainingLeaseTimeString(now: Long = -1) : String
    {
        if (this.LeaseDuration == 0)
        {
            return "Never"
        }
        // show only 2 units (i.e. days and hours. or hours and minutes. or minutes and seconds. or just seconds)
        val totalSecs = getRemainingLeaseTime(now)
        val dhms = getDHMS(totalSecs)
        val timeLeftString = roundTwoUnits(dhms)

        return if (totalSecs > 0) {
            timeLeftString
        } else {
            "Expired"
        }
    }

    fun getUrgency(autoRenew : Boolean, now : Long = -1) : Urgency
    {
        if (autoRenew)
        {
            return Urgency.Normal
        }
        val totalSecs = getRemainingLeaseTime(now)
        val totalMinutes = totalSecs / 60
        if (totalMinutes <= 1)
        {
            return Urgency.Error
        }
        else if (totalMinutes <= 5)
        {
            return Urgency.Warn
        }
        return Urgency.Normal
    }

    fun toStringFull() : String
    {
        return "PortMapping(RemoteHost=$RemoteHost, ActualExternalIP=$DeviceIP, InternalIP=$InternalIP, ExternalPort=$ExternalPort, InternalPort=$InternalPort, Protocol=$Protocol, Enabled=$Enabled, LeaseDuration=$LeaseDuration, Description=$Description, TimeReadLeaseDurationMs=$TimeReadLeaseDurationMs, Slot=$Slot)"
    }
}

enum class Urgency {
    Normal,
    Warn,
    Error,
}

data class DayHourMinSec(val days : Int, val hours : Int, val minutes : Int, val seconds : Int)
{
    fun totalSeconds() : Int
    {
        return days * 3600 * 24 + hours * 3600 + minutes * 60 + seconds
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

fun getPlural(value : Int) : String
{
    return if (value > 1) "s" else ""
}

fun roundOneUnit(time: DayHourMinSec): String {
    var days = time.days
    var hours = time.hours
    var minutes = time.minutes
    val seconds = time.seconds
    return when {
        days > 0 -> {
            val remainderSec = hours * 3600 + minutes * 60 + seconds
            if (remainderSec >= 12 * 3600 * hours) {
                days += 1
            }
            "$days day${getPlural(days)}"
        }
        hours > 0 -> {
            val remainderSec = minutes * 60 + seconds
            if (remainderSec >= 1800) {
                hours += 1
            }
            if (hours == 24) {
                return "1 day"
            }
            return "$hours hour${getPlural(hours)}"
        }
        minutes > 0 -> {
            if (seconds >= 30) {
                minutes += 1
            }
            if (minutes == 60) {
                return "1 hour"
            }

            return "$minutes minute${getPlural(minutes)}"
        }
        else -> "$seconds second${getPlural(seconds)}"
    }
}

fun roundTwoUnits(time: DayHourMinSec): String {
    var days = time.days
    var hours = time.hours
    var minutes = time.minutes
    val seconds = time.seconds
    return when {
        days > 0 -> {
            val remainderSec = minutes * 60 + seconds
            if (remainderSec >= 30 * 60) {
                hours += 1
                if (hours == 24) { days += 1; hours = 0 }
            }
            "$days day${getPlural(days)}, $hours hour${getPlural(hours)}"
        }
        hours > 0 -> {
            if (seconds >= 30) {
                minutes += 1
                if (minutes == 60) { hours += 1; minutes = 0 }
            }
            return "$hours hour${getPlural(hours)}, $minutes minute${getPlural(minutes)}"
        }
        minutes > 0 -> {
            return "$minutes minute${getPlural(minutes)}, $seconds second${getPlural(seconds)}"
        }
        else -> "$seconds second${getPlural(seconds)}"
    }
}

fun formatShortName(protocol: String, externalIp: String, externalPort: String) : String
{
    return "$protocol rule at $externalIp:$externalPort"
}

fun PortMappingEntity.getPrefs(): PortMappingPref = PortMappingPref(this.autoRenew, this.desiredLeaseDuration)