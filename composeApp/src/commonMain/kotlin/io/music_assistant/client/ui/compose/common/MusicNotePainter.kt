package io.music_assistant.client.ui.compose.common

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter

class MusicNotePainter(
    private val backgroundColor: Color = Color(0xFFE8E8E8),
    private val iconColor: Color = Color(0xFF9E9E9E),
) : Painter() {
    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw background
        drawRect(
            color = backgroundColor,
            size = Size(canvasWidth, canvasHeight),
        )

        // Draw music note
        val centerX = canvasWidth / 2
        val centerY = canvasHeight / 2
        val noteSize = minOf(canvasWidth, canvasHeight) * 0.5f

        // Note head (circle)
        val noteHeadRadius = noteSize * 0.15f
        val noteHeadX = centerX - noteSize * 0.1f
        val noteHeadY = centerY + noteSize * 0.2f

        drawCircle(
            color = iconColor,
            radius = noteHeadRadius,
            center = Offset(noteHeadX, noteHeadY),
        )

        // Note stem (vertical line)
        val stemWidth = noteSize * 0.04f
        val stemHeight = noteSize * 0.6f
        val stemX = noteHeadX + noteHeadRadius * 0.8f
        val stemTop = noteHeadY - stemHeight

        drawRect(
            color = iconColor,
            topLeft = Offset(stemX, stemTop),
            size = Size(stemWidth, stemHeight),
        )

        // Note flag (curved shape)
        val flagPath =
            Path().apply {
                moveTo(stemX + stemWidth, stemTop)
                quadraticTo(
                    stemX + noteSize * 0.3f,
                    stemTop + noteSize * 0.1f,
                    stemX + noteSize * 0.25f,
                    stemTop + noteSize * 0.25f,
                )
                quadraticTo(
                    stemX + noteSize * 0.15f,
                    stemTop + noteSize * 0.35f,
                    stemX + stemWidth,
                    stemTop + noteSize * 0.3f,
                )
                close()
            }

        drawPath(
            path = flagPath,
            color = iconColor,
        )
    }
}
