package io.music_assistant.client.utils

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class ExtTest {
    @Test
    fun testValidIpAddresses() {
        val validIps = listOf(
            "192.168.1.1",
            "0.0.0.0",
            "255.255.255.255",
            "127.0.0.1",
            "10.0.0.1"
        )
        for (ip in validIps) {
            assertTrue(ip.isIpAddress(), "Expected $ip to be recognised as a valid IP address")
        }
    }

    @Test
    fun testInvalidIpAddresses() {
        val invalidIps = listOf(
            "256.256.256.256",
            "192.168.1",
            "192.168.1.1.1",
            "abc.def.ghi.jkl",
            "123.456.78.90",
            "192.168.1.-1",
            "192.168.1.256",
            "",
            "...",
            "1.1.1.01"
        )
        for (ip in invalidIps) {
            assertFalse(ip.isIpAddress(), "Expected $ip to be recognised as an invalid IP address")
        }
    }

    @Test
    fun testValidIpPorts() {
        val validPorts = listOf("1", "80", "65535", "12345")
        for (port in validPorts) {
            assertTrue(port.isIpPort(), "Expected $port to be recognised as a valid port")
        }
    }

    @Test
    fun testInvalidIpPorts() {
        val invalidPorts = listOf("0", "65536", "-1", "abc", "", "99999")
        for (port in invalidPorts) {
            assertFalse(port.isIpPort(), "Expected $port to be recognised as an invalid port")
        }
    }

    @Test
    fun testToMinSec() {
        assertEquals("1:05", 65.seconds.toMinSec())
        assertEquals("0:00", 0.seconds.toMinSec())
        assertEquals("--:--", (null as Duration?).toMinSec())
        assertEquals("10:00", Duration.parse("600s").toMinSec())
    }
} 