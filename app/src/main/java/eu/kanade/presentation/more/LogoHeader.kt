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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

// Brand palette — matches the SVG exactly
private val Parchment0 = Color(0xFFFDF6EC)
private val Parchment1 = Color(0xFFEDE4D4)
private val InkRed0    = Color(0xFFE8341A)
private val InkRed1    = Color(0xFFC01E0E)
private val InkRed2    = Color(0xFF7A0C06)
private val BracketCol = Color(0xFFC8A880)

@Composable
fun LogoHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        ) {
            val w = size.width
            val h = size.height

            // ── BACKGROUND: parchment radial gradient ──────────────────────
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Parchment0, Parchment1),
                    center = Offset(w * 0.50f, h * 0.46f),
                    radius = maxOf(w, h) * 0.85f,
                ),
            )

            // ── CORNER BRACKETS (manga panel detail) ──────────────────────
            val bLen = w * 0.065f
            val bOff = w * 0.045f
            val bW   = 2.4f
            val bCol = BracketCol.copy(alpha = 0.42f)
            drawLine(bCol, Offset(bOff, bOff),         Offset(bOff, bOff + bLen),     strokeWidth = bW)
            drawLine(bCol, Offset(bOff, bOff),         Offset(bOff + bLen, bOff),     strokeWidth = bW)
            drawLine(bCol, Offset(w - bOff, bOff),     Offset(w - bOff, bOff + bLen), strokeWidth = bW)
            drawLine(bCol, Offset(w - bOff, bOff),     Offset(w - bOff - bLen, bOff), strokeWidth = bW)
            drawLine(bCol, Offset(bOff, h - bOff),     Offset(bOff, h - bOff - bLen), strokeWidth = bW)
            drawLine(bCol, Offset(bOff, h - bOff),     Offset(bOff + bLen, h - bOff), strokeWidth = bW)
            drawLine(bCol, Offset(w - bOff, h - bOff), Offset(w - bOff, h - bOff - bLen), strokeWidth = bW)
            drawLine(bCol, Offset(w - bOff, h - bOff), Offset(w - bOff - bLen, h - bOff), strokeWidth = bW)

            // ── TOP BRUSH STROKES ──────────────────────────────────────────
            // SVG proportions: strokes at ~27–36% height, overlap character top
            val tY = h * 0.305f

            fun topPath() = Path().apply {
                moveTo(w * 0.090f, tY + h * 0.008f)
                quadraticTo(w * 0.215f, tY - h * 0.092f, w * 0.385f, tY - h * 0.010f)
                quadraticTo(w * 0.555f, tY + h * 0.052f, w * 0.680f, tY - h * 0.058f)
                quadraticTo(w * 0.800f, tY - h * 0.114f, w * 0.925f, tY - h * 0.046f)
            }

            // glow underlay
            drawPath(topPath(), InkRed2.copy(alpha = 0.22f),
                style = Stroke(width = w * 0.095f, cap = StrokeCap.Round))
            // main stroke
            drawPath(topPath(),
                brush = Brush.linearGradient(listOf(InkRed0, InkRed1, InkRed2),
                    Offset(0f, tY), Offset(w, tY)),
                style = Stroke(width = w * 0.042f, cap = StrokeCap.Round))
            // hair highlight
            drawPath(topPath(), Color(0xFFFF6040).copy(alpha = 0.35f),
                style = Stroke(width = w * 0.005f, cap = StrokeCap.Round))

            // secondary thinner parallel
            val tY2 = tY + h * 0.024f
            val topSecond = Path().apply {
                moveTo(w * 0.135f, tY2 + h * 0.006f)
                quadraticTo(w * 0.255f, tY2 - h * 0.058f, w * 0.405f, tY2 + h * 0.018f)
                quadraticTo(w * 0.570f, tY2 + h * 0.062f, w * 0.695f, tY2 - h * 0.030f)
                quadraticTo(w * 0.815f, tY2 - h * 0.076f, w * 0.935f, tY2 + h * 0.002f)
            }
            drawPath(topSecond,
                brush = Brush.linearGradient(listOf(InkRed0, InkRed1, InkRed2),
                    Offset(0f, tY2), Offset(w, tY2)),
                alpha = 0.55f,
                style = Stroke(width = w * 0.019f, cap = StrokeCap.Round))

            // ── 漫 CHARACTER ──────────────────────────────────────────────
            val charY = h * 0.738f
            drawIntoCanvas { canvas ->
                // shadow pass (offset, no shader)
                val shadowPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.argb(46, 138, 16, 8)
                    textSize = h * 0.560f
                    typeface = Typeface.create("serif", Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("漫", w * 0.50f + 3f, charY + 5f, shadowPaint)
                // main pass
                val charPaint = android.graphics.Paint().apply {
                    color = 0xFF1C0E0A.toInt()
                    textSize = h * 0.560f
                    typeface = Typeface.create("serif", Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                canvas.nativeCanvas.drawText("漫", w * 0.50f, charY, charPaint)
            }

            // ── BOTTOM BRUSH STROKES (drawn OVER character) ───────────────
            val bY = h * 0.780f

            fun botPath() = Path().apply {
                moveTo(w * 0.060f, bY + h * 0.002f)
                quadraticTo(w * 0.275f, bY + h * 0.098f, w * 0.505f, bY + h * 0.028f)
                quadraticTo(w * 0.725f, bY - h * 0.040f, w * 0.862f, bY + h * 0.062f)
                quadraticTo(w * 0.920f, bY + h * 0.090f, w * 0.964f, bY + h * 0.040f)
            }

            // glow underlay
            drawPath(botPath(), InkRed2.copy(alpha = 0.20f),
                style = Stroke(width = w * 0.110f, cap = StrokeCap.Round))
            // main stroke
            drawPath(botPath(),
                brush = Brush.linearGradient(listOf(InkRed2, InkRed1, InkRed0),
                    Offset(0f, bY), Offset(w, bY)),
                style = Stroke(width = w * 0.050f, cap = StrokeCap.Round))
            // hair highlight
            drawPath(botPath(), Color(0xFFFF5030).copy(alpha = 0.30f),
                style = Stroke(width = w * 0.006f, cap = StrokeCap.Round))

            // secondary thinner parallel
            val bY2 = bY - h * 0.022f
            val botSecond = Path().apply {
                moveTo(w * 0.090f, bY2 - h * 0.004f)
                quadraticTo(w * 0.295f, bY2 + h * 0.062f, w * 0.515f, bY2 + h * 0.006f)
                quadraticTo(w * 0.725f, bY2 - h * 0.055f, w * 0.855f, bY2 + h * 0.028f)
                quadraticTo(w * 0.905f, bY2 + h * 0.050f, w * 0.942f, bY2 + h * 0.016f)
            }
            drawPath(botSecond,
                brush = Brush.linearGradient(listOf(InkRed2, InkRed1, InkRed0),
                    Offset(0f, bY2), Offset(w, bY2)),
                alpha = 0.50f,
                style = Stroke(width = w * 0.020f, cap = StrokeCap.Round))

            // ── RULE LINE ─────────────────────────────────────────────────
            val ruleY = h * 0.862f
            drawLine(
                Color(0xFFC8A070).copy(alpha = 0.50f),
                Offset(w * 0.20f, ruleY),
                Offset(w * 0.80f, ruleY),
                strokeWidth = 1.5f,
            )

            // ── FORGE + TAGLINE ───────────────────────────────────────────
            drawIntoCanvas { canvas ->
                // FORGE
                val forgePaint = android.graphics.Paint().apply {
                    shader = android.graphics.LinearGradient(
                        0f, h * 0.875f, 0f, h * 0.960f,
                        intArrayOf(0xFFC8860A.toInt(), 0xFFA04808.toInt(), 0xFF7A1E06.toInt()),
                        floatArrayOf(0f, 0.5f, 1f),
                        android.graphics.Shader.TileMode.CLAMP,
                    )
                    textSize     = h * 0.095f
                    typeface     = Typeface.create("serif", Typeface.BOLD)
                    textAlign    = android.graphics.Paint.Align.CENTER
                    isAntiAlias  = true
                    letterSpacing = 0.28f
                    setShadowLayer(h * 0.009f, 1f, 2f,
                        android.graphics.Color.argb(64, 122, 30, 6))
                }
                canvas.nativeCanvas.drawText("FORGE", w * 0.50f, h * 0.948f, forgePaint)

                // READ · COLLECT · DISCOVER
                val tagPaint = android.graphics.Paint().apply {
                    color         = android.graphics.Color.argb(190, 160, 144, 128)
                    textSize      = h * 0.038f
                    typeface      = Typeface.MONOSPACE
                    textAlign     = android.graphics.Paint.Align.CENTER
                    isAntiAlias   = true
                    letterSpacing = 0.20f
                }
                canvas.nativeCanvas.drawText(
                    "READ  \u00B7  COLLECT  \u00B7  DISCOVER",
                    w * 0.50f, h * 0.984f, tagPaint,
                )
            }
        }

        HorizontalDivider()
    }
}
