package com.shinjiindustrial.portmapper

import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Test

class LogFilterTests {
    @Test
    fun `empty query matches everything`() {
        assertTrue(matchesLogFilter("I: Device found", ""))
    }

    @Test
    fun `smart case queries`() {
        assertTrue(matchesLogFilter("E: AddPortMapping failed", "E: "))
        assertTrue(matchesLogFilter("12:00:01.234 E: AddPortMapping failed", "E: "))
        assertTrue(matchesLogFilter("I: UPnP device found", "upnp"))
        assertTrue(matchesLogFilter("I: upnp device found", "upnp"))
        assertFalse(matchesLogFilter("I: upnp device found", "UPnP"))
        assertFalse(matchesLogFilter("test", "E"))
        assertFalse(matchesLogFilter("I: Failure: 718 ConflictInMappingEntry", "E: "))
        assertFalse(matchesLogFilter("I: Device: 192.168.1.1", "E: "))
    }
}
