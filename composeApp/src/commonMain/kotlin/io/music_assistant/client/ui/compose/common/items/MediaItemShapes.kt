package io.music_assistant.client.ui.compose.common.items

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

/**
 * Shape that cuts a vertical strip from the right side.
 * Used for album vinyl record effect.
 */
class CutStripShape(private val stripWidth: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        return Outline.Generic(Path().apply {
            val stripPx = with(density) { stripWidth.toPx() }

            // Defines the album cover area, excluding the rightmost strip
            moveTo(0f, 0f)
            lineTo(size.width - stripPx, 0f)
            lineTo(size.width - stripPx, size.height)
            lineTo(0f, size.height)
            close()
        })
    }
}

/**
 * Shape that cuts a circular hole in the center.
 * Used for album vinyl record effect.
 */
class HoleShape(private val holeRadius: Dp) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val radiusPx = with(density) { holeRadius.toPx() }
        val center = size.width / 2f

        // Define the full cover path
        val coverPath = Path().apply {
            addRect(Rect(Offset.Zero, size))
        }

        // Define the hole path
        val holePath = Path().apply {
            val rect = Rect(
                left = center - radiusPx,
                top = center - radiusPx,
                right = center + radiusPx,
                bottom = center + radiusPx
            )
            addOval(oval = rect)
        }

        // Subtract hole from cover
        val finalPath = Path.combine(
            operation = PathOperation.Difference,
            path1 = coverPath,
            path2 = holePath
        )

        return Outline.Generic(finalPath)
    }
}
