package ua.pp.formatbce.musicassistant.ui.compose.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ActionIcon(
    icon: ImageVector,
    size: Dp = 24.dp,
    tint: Color = MaterialTheme.colors.primary,
    enabled: Boolean = true,
    onClick: () -> Unit
) {

    Icon(
        modifier = Modifier
            .padding(horizontal = 6.dp)
            .clickable(enabled = enabled) { onClick() }
            .alpha(if (enabled) 1F else 0.5f)
            .size(size)
            .padding(all = 2.dp),
        imageVector = icon,
        contentDescription = null,
        tint = tint,
    )
}