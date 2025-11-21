package io.music_assistant.client.utils

import androidx.compose.ui.Modifier
import kotlin.time.Duration

fun Duration?.toMinSec() =
    this?.let {
        it.inWholeMinutes.toString() +
            ":" +
            (it.inWholeSeconds % 60)
                .toString()
                .padStart(2, '0')
    } ?: "--:--"

fun String.isValidHost(): Boolean {
    val ipv4Pattern =
        Regex(
            """^(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$""".replace(Regex("\\s"), ""),
        )
    val hostnamePattern =
        Regex(
            """^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z0-9-]{1,63})*
        (?<!\.)$""".replace(Regex("\\s"), ""),
        )

    // Check IPv4 first
    if (this.matches(ipv4Pattern)) {
        return true
    }

    // Reject anything that looks like an IP address structure (numeric or 4 dot-separated segments)
    // but doesn't match the valid IPv4 pattern
    val segments = this.split(".")

    // If all segments are numeric, it's an invalid IP attempt
    if (segments.all { it.toIntOrNull() != null }) {
        return false
    }

    // If exactly 4 segments and all are purely alphanumeric with 3 chars each,
    // it might be mistaken for an IP-like pattern, reject it
    if (segments.size == 4 && segments.all { it.length <= 3 && it.all { c -> c.isLetterOrDigit() } }) {
        // Allow if at least one segment has letters (making it clearly not an IP attempt)
        val hasLetters = segments.any { segment -> segment.any { it.isLetter() } }
        if (!hasLetters) {
            return false
        }
        // If it's 4 segments of short alphanumeric strings, it's ambiguous - reject
        // unless it has clear hostname characteristics (like longer segments or hyphens)
        val hasHostnameCharacteristics = segments.any { it.length > 3 || it.contains('-') }
        if (!hasHostnameCharacteristics) {
            return false
        }
    }

    return this.matches(hostnamePattern)
}

fun String.isIpPort(): Boolean {
    val port = this.toIntOrNull()
    return port != null && port in 1..65535
}

fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: (Modifier.() -> Modifier)? = null,
): Modifier =
    if (condition) {
        then(ifTrue(Modifier))
    } else if (ifFalse != null) {
        then(ifFalse(Modifier))
    } else {
        this
    }
