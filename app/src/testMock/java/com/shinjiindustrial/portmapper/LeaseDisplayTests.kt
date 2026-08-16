package com.shinjiindustrial.portmapper

import com.shinjiindustrial.portmapper.domain.PortMapping
import com.shinjiindustrial.portmapper.domain.PortMappingWithPref
import junit.framework.TestCase.assertEquals
import org.junit.Test

class LeaseDisplayTests {

    private fun rule(leaseDuration: Int) = PortMappingWithPref(
        PortMapping(
            "Rule", "", "192.168.1.13", 5015, 5015, "TCP", true, leaseDuration,
            "192.168.18.1", 0L, 1
        )
    )

    @Test
    fun `expired rule reads Expired`() {
        val oneSecondPastExpiry = 3601L * 1000
        assertEquals(
            "Expired",
            rule(3600).getRemainingLeaseOrRenewTimeRoughString(oneSecondPastExpiry)
        )
    }

    @Test
    fun `at exactly zero remaining the row and the detail string agree`() {
        val atExpiry = 3600L * 1000
        val expiredRule = rule(3600)

        assertEquals("Expired", expiredRule.getRemainingLeaseOrRenewTimeRoughString(atExpiry))
        assertEquals("Expired", expiredRule.portMapping.getRemainingLeaseTimeString(atExpiry))
    }

    @Test
    fun `expires in under a minute`() {
        val thirtySecondsLeft = 3570L * 1000
        assertEquals(
            "Expires in <1 minute",
            rule(3600).getRemainingLeaseOrRenewTimeRoughString(thirtySecondsLeft)
        )
    }

    @Test
    fun `rule with no lease duration never expires`() {
        assertEquals("Expires Never", rule(0).getRemainingLeaseOrRenewTimeRoughString(0L))
    }
}
