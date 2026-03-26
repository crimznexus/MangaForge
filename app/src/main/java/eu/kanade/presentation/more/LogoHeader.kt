package eu.kanade.presentation.more

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

private val Purple0 = Color(0xFF8477FF)
private val Purple1 = Color(0xFF5A18C0)

@Composable
fun LogoHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val w = size.width
            val h = size.height

            // Background: purple gradient
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Purple0, Purple1),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                ),
            )

            // Three diagonal white strokes (氵 water radical motif) — left of character
            val strokeColor = Color(0x99FFFFFF)
            val lineW = w * 0.025f
            // Upper stroke
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.08f, h * 0.20f),
                end   = Offset(w * 0.19f, h * 0.36f),
                strokeWidth = lineW,
                cap = StrokeCap.Round,
            )
            // Middle stroke
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.06f, h * 0.43f),
                end   = Offset(w * 0.17f, h * 0.59f),
                strokeWidth = lineW,
                cap = StrokeCap.Round,
            )
            // Lower stroke
            drawLine(
                color = strokeColor,
                start = Offset(w * 0.05f, h * 0.66f),
                end   = Offset(w * 0.16f, h * 0.82f),
                strokeWidth = lineW,
                cap = StrokeCap.Round,
            )

            // 漫 character — white, stroke style (outline)
            val charY = h * 0.68f
            drawIntoCanvas { canvas ->
                val charPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = h * 0.52f
                    typeface = Typeface.create("serif", Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    style = android.graphics.Paint.Style.STROKE
                    strokeWidth = h * 0.012f
                }
                canvas.nativeCanvas.drawText("漫", w * 0.58f, charY, charPaint)

                // "MangaForge" title — white, bold
                val titlePaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = h * 0.115f
                    typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    letterSpacing = 0.04f
                }
                canvas.nativeCanvas.drawText("MangaForge", w * 0.50f, h * 0.88f, titlePaint)

                // Tagline — semi-transparent white
                val tagPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(180, 255, 255, 255)
                    textSize = h * 0.050f
                    typeface = Typeface.MONOSPACE
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    letterSpacing = 0.18f
                }
                canvas.nativeCanvas.drawText(
                    "READ  \u00B7  COLLECT  \u00B7  DISCOVER",
                    w * 0.50f, h * 0.965f, tagPaint,
                )
            }
        }

        HorizontalDivider()
    }
}
