package io.music_assistant.client.ui.compose.common.painters

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

class VinylRecordPainter(
    private val recordColor: Color,
    private val labelColor: Color,
    private val holeColor: Color,
    private val innerCircleRadius: Dp = 16.dp, // Color for the center spindle hole
    private val holeRadius: Dp = 3.dp, // Color for the center spindle hole
    private val grooveColor: Color = labelColor.copy(alpha = 0.4f), // Subtle white for grooves
    private val grooveCount: Int = 6 // Number of grooves to draw
) : Painter() {

    private var cachedSize: Size? = null
    private var cachedGrooveRadii: List<Float>? = null

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        drawCircle(
            color = recordColor,
            radius = radius,
            center = center,
            style = Fill
        )

        val labelRadius = radius * 0.45f
        drawCircle(
            color = labelColor,
            radius = labelRadius,
            center = center,
            style = Fill
        )

        drawCircle(
            color = recordColor,
            radius = with(Density(density)) { innerCircleRadius.toPx() },
            center = center,
            style = Fill
        )

        drawCircle(
            color = labelColor,
            radius = with(Density(density)) { (innerCircleRadius - 3.dp).toPx() },
            center = center,
            style = Fill
        )

        drawCircle(
            color = holeColor,
            radius = with(Density(density)) { holeRadius.toPx() },
            center = center,
            style = Fill
        )

        val cache = cachedGrooveRadii
        val grooveRadii = if (cachedSize == size && cache != null) {
            cache
        } else {
            calculateGrooveRadii(labelRadius, radius).also {
                cachedSize = size
                cachedGrooveRadii = it
            }
        }

        val strokeWidth = 0.5.dp.toPx()
        for (grooveRadius in grooveRadii) {
            drawCircle(
                color = grooveColor,
                radius = grooveRadius,
                center = center,
                style = Stroke(width = strokeWidth)
            )
        }
    }

    private fun DrawScope.calculateGrooveRadii(labelRadius: Float, radius: Float): List<Float> {
        val grooveStartRadius = labelRadius + 1.dp.toPx()
        val grooveEndRadius = radius - 1.dp.toPx()

        return if (grooveEndRadius > grooveStartRadius) {
            val grooveSpacing = (grooveEndRadius - grooveStartRadius) / grooveCount
            List(grooveCount) { i ->
                grooveStartRadius + (i * grooveSpacing)
            }
        } else {
            emptyList()
        }
    }
}