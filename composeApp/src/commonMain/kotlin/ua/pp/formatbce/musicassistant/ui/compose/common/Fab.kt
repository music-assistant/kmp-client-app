package ua.pp.formatbce.musicassistant.ui.compose.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun Fab(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    text: String,
    onClick: () -> Unit = {},
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it * 2 }),
        exit = slideOutVertically(targetOffsetY = { it * 2 }),
    ) {
        ExtendedFloatingActionButton(
            modifier = modifier,
            onClick = onClick,
            text = {
                Text(
                    text = text,
                    style = MaterialTheme.typography.button
                )
            }
        )
    }
}