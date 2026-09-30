package com.shinjiindustrial.portmapper.background

import com.shinjiindustrial.portmapper.domain.DeviceStatus
import com.shinjiindustrial.portmapper.domain.IIGDDevice
import com.shinjiindustrial.portmapper.domain.PortMappingKey
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import com.shinjiindustrial.portmapper.persistence.PortMappingEntity

// Rule must be ours, with auto renew rule on, finite lease
fun PortMappingWithPref.isLiveAutoRenew(): Boolean {
    return portMappingPref != null &&
            getAutoRenewOrDefault() &&
            portMapping.Enabled &&
            portMapping.LeaseDuration != 0
}

// The elapsedRealtime by which the next background renew must happen. Considers both live rules
//   and stores rules (checking their recency with lastSeen + leaseDuration)
fun backgroundRenewDeadline(
    devices: List<IIGDDevice>,
    portMappings: Map<PortMappingKey, PortMappingWithPref>,
    storedRows: List<PortMappingEntity>,
    nowElapsedMs: Long,
    nowUtcMs: Long,
): Long? {
    val enumerated = devices.filter { it.status == DeviceStatus.FinishedEnumeratingMappings }
    val enumeratedIps = enumerated.map { it.getIpAddress() }.toSet()
    val enumeratedUdns = enumerated.map { it.udn }.toSet()

    // live rules on the live router
    val fromRouter = portMappings.values
        .filter { it.portMapping.DeviceIP in enumeratedIps && it.isLiveAutoRenew() }
        // already expired we can ignore (only live rules)
        .filter { it.portMapping.getExpiresTimeMillis() > nowElapsedMs }
        .map { it.portMapping.getExpiresTimeMillis() - RenewScheduler.marginMs(it.getDesiredLeaseDurationOrDefault()) }

    // stored rules which are "live" in the sense that they would still be alive (even if we are
    //   not currently connected to the router in question OR if we just didnt connect yet)
    val fromStore = storedRows
        .filter {
            it.deviceSignature !in enumeratedUdns &&
                    it.autoRenew &&
                    it.desiredLeaseDuration > 0 &&
                    it.lastSeenAtUtcMs != null
        }
        .mapNotNull { row ->
            val remainingMs = row.lastSeenAtUtcMs!! + row.desiredLeaseDuration * 1000L - nowUtcMs
            if (remainingMs <= 0) {
                null
            } else {
                nowElapsedMs + remainingMs - RenewScheduler.marginMs(row.desiredLeaseDuration)
            }
        }

    return (fromRouter + fromStore).minOrNull()
}
