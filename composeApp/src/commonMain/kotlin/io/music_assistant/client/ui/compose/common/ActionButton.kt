package io.music_assistant.client.ui.compose.common

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ActionButton(
    icon: ImageVector,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    IconButton(
        modifier = Modifier
            .alpha(if (enabled) 1F else 0.5f)
            .size(size),
        onClick = onClick,
        enabled = enabled,
    ) {
        Icon(
            modifier = Modifier.size(size),
            imageVector = icon,
            contentDescription = null,
            tint = tint,
        )
    }
}