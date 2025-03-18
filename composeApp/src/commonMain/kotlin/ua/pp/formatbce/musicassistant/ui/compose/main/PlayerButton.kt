package ua.pp.formatbce.musicassistant.ui.compose.main

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp

data class PlayerButton(
    val action: PlayerAction,
    val iconVector: ImageVector,
    val size: Dp,
)
