package io.music_assistant.client.ui.compose.common.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MusicMicrophonePainter(
    private val backgroundColor: Color = Color(0xFFE8E8E8),
    private val iconColor: Color = Color(0xFF424242)
) : Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Draw background
        drawRect(color = backgroundColor, size = Size(canvasWidth, canvasHeight))

        val minDim = minOf(canvasWidth, canvasHeight)
        val scale = minDim * 0.5f

        val centerX = canvasWidth / 2f
        val centerY = canvasHeight / 2f

        // Rotation angle for the entire microphone
        val angle = -15f
        val angleRad = angle * PI.toFloat() / 180f

        // Classic handheld microphone proportions
        val headRadius = scale * 0.25f
        val handleTopWidth = scale * 0.13f  // Narrower at bottom (cable end)
        val handleBottomWidth = scale * 0.18f  // Wider at top (mesh end)
        val handleHeight = scale * 0.6f

        // Calculate positions along the rotated axis
        val micTotalLength = headRadius + handleHeight
        val headOffset = micTotalLength / 2f - headRadius * 0.7f
        val handleCenterOffset = headOffset - headRadius * 0.5f - handleHeight / 2f

        // Head center position (rotated)
        val headCenterX = centerX - sin(angleRad) * headOffset
        val headCenterY = centerY - cos(angleRad) * headOffset

        // --- 1. Draw the rounded mesh head ---
        drawCircle(
            color = iconColor,
            radius = headRadius,
            center = Offset(headCenterX, headCenterY)
        )

        // Add mesh lines to the head
        val meshLines = 5
        for (i in -meshLines..meshLines) {
            val yOffset = (i.toFloat() / meshLines) * headRadius * 0.7f
            val lineWidth = sqrt(headRadius * headRadius - yOffset * yOffset) * 1.6f

            // Rotate mesh lines with the microphone
            val lineAngleRad = angleRad
            val perpX = cos(lineAngleRad)
            val perpY = sin(lineAngleRad)
            val alongX = -sin(lineAngleRad)
            val alongY = cos(lineAngleRad)

            drawLine(
                color = backgroundColor,
                start = Offset(
                    headCenterX - perpX * lineWidth / 2 + alongX * yOffset,
                    headCenterY - perpY * lineWidth / 2 + alongY * yOffset
                ),
                end = Offset(
                    headCenterX + perpX * lineWidth / 2 + alongX * yOffset,
                    headCenterY + perpY * lineWidth / 2 + alongY * yOffset
                ),
                strokeWidth = scale * 0.01f
            )
        }

        // --- 2. Draw the tapered handle ---
        val handleCenterX = centerX - sin(angleRad) * handleCenterOffset
        val handleCenterY = centerY - cos(angleRad) * handleCenterOffset

        // Create trapezoid path for tapered handle
        val handlePath = Path().apply {
            // Calculate corners of the trapezoid
            val topLeft = Offset(
                handleCenterX - cos(angleRad) * handleTopWidth / 2 + sin(angleRad) * handleHeight / 2,
                handleCenterY + sin(angleRad) * handleTopWidth / 2 + cos(angleRad) * handleHeight / 2
            )
            val topRight = Offset(
                handleCenterX + cos(angleRad) * handleTopWidth / 2 + sin(angleRad) * handleHeight / 2,
                handleCenterY - sin(angleRad) * handleTopWidth / 2 + cos(angleRad) * handleHeight / 2
            )
            val bottomRight = Offset(
                handleCenterX + cos(angleRad) * handleBottomWidth / 2 - sin(angleRad) * handleHeight / 2,
                handleCenterY - sin(angleRad) * handleBottomWidth / 2 - cos(angleRad) * handleHeight / 2
            )
            val bottomLeft = Offset(
                handleCenterX - cos(angleRad) * handleBottomWidth / 2 - sin(angleRad) * handleHeight / 2,
                handleCenterY + sin(angleRad) * handleBottomWidth / 2 - cos(angleRad) * handleHeight / 2
            )

            moveTo(topLeft.x, topLeft.y)
            lineTo(topRight.x, topRight.y)
            lineTo(bottomRight.x, bottomRight.y)
            lineTo(bottomLeft.x, bottomLeft.y)
            close()
        }

        drawPath(
            path = handlePath,
            color = iconColor
        )

        // Add grip rings on the handle (rotated)
        val ring1Offset = handleHeight * 0.15f
        val ring2Offset = 0f
        val ring3Offset = -handleHeight * 0.15f

        listOf(ring1Offset, ring2Offset, ring3Offset).forEach { offset ->
            val ringCenterX = handleCenterX - sin(angleRad) * offset
            val ringCenterY = handleCenterY - cos(angleRad) * offset

            // Calculate width at this position (linear interpolation)
            val t = (offset + handleHeight / 2) / handleHeight
            val ringWidth = handleBottomWidth + (handleTopWidth - handleBottomWidth) * t

            drawLine(
                color = backgroundColor,
                start = Offset(
                    ringCenterX - cos(angleRad) * ringWidth / 2,
                    ringCenterY + sin(angleRad) * ringWidth / 2
                ),
                end = Offset(
                    ringCenterX + cos(angleRad) * ringWidth / 2,
                    ringCenterY - sin(angleRad) * ringWidth / 2
                ),
                strokeWidth = scale * 0.015f
            )
        }

        // --- 3. Draw the cable coming from bottom ---
        val cableStartOffset = handleCenterOffset - handleHeight / 2f
        val cableStartX = centerX - sin(angleRad) * cableStartOffset
        val cableStartY = centerY - cos(angleRad) * cableStartOffset

        val cablePath = Path().apply {
            moveTo(cableStartX, cableStartY)

            // Create a curved cable
            val controlX1 = cableStartX + scale * 0.1f
            val controlY1 = cableStartY + scale * 0.15f
            val controlX2 = cableStartX + scale * 0.05f
            val controlY2 = cableStartY + scale * 0.3f
            val endX = cableStartX - scale * 0.05f
            val endY = cableStartY + scale * 0.35f

            cubicTo(controlX1, controlY1, controlX2, controlY2, endX, endY)
        }

        drawPath(
            path = cablePath,
            color = iconColor,
            style = Stroke(width = scale * 0.025f, cap = StrokeCap.Round)
        )
    }
}