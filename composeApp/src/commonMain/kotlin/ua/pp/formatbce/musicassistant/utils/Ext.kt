package ua.pp.formatbce.musicassistant.utils

import kotlin.time.Duration

fun Duration?.toMinSec() =
    this?.let {
        it.inWholeMinutes.toString() +
                ":" +
                (it.inWholeSeconds % 60).toString()
                    .padStart(2, '0')
    } ?: "--:--"

fun String.isIpAddress(): Boolean {
    val ipv4Pattern = Regex(
        """^(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$""".replace(Regex("\\s"), "")
    )
    return this.matches(ipv4Pattern)
}

fun String.isIpPort() : Boolean {
    val port = this.toIntOrNull()
    return port != null && port in 1..65535
}
