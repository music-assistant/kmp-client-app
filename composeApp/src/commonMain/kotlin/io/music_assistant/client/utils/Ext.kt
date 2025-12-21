package io.music_assistant.client.utils

import androidx.compose.ui.Modifier
import io.music_assistant.client.api.Answer
import kotlin.time.Duration

fun Duration?.toMinSec() =
    this?.let {
        it.inWholeMinutes.toString() +
                ":" +
                (it.inWholeSeconds % 60).toString()
                    .padStart(2, '0')
    } ?: "--:--"

fun String.isValidHost(): Boolean {
    val ipv4Pattern = Regex(
        """^(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\.
           (25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$""".replace(Regex("\\s"), "")
    )
    val hostnamePattern = Regex("""^(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\.[A-Za-z0-9-]{1,63})*
        (?<!\.)$""".replace(Regex("\\s"), ""))
    return this.matches(ipv4Pattern) || this.matches(hostnamePattern)
}

fun String.isIpPort() : Boolean {
    val port = this.toIntOrNull()
    return port != null && port in 1..65535
}

fun Modifier.conditional(
    condition: Boolean,
    ifTrue: Modifier.() -> Modifier,
    ifFalse: (Modifier.() -> Modifier)? = null,
): Modifier {
    return if (condition) {
        then(ifTrue(Modifier))
    } else if (ifFalse != null) {
        then(ifFalse(Modifier))
    } else {
        this
    }
}

inline fun <reified T : Any> Result<Answer>.resultAs(): T?  = getOrNull()?.resultAs()

