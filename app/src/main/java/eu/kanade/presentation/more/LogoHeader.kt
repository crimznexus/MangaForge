package eu.kanade.presentation.more

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.dp

private val LogoTeal  = Color(0xFF4FD1C5)
private val LogoMid   = Color(0xFF319795)
private val LogoDeep  = Color(0xFF2C5282)

@Composable
fun LogoHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
        ) {
            val w = size.width
            val h = size.height
            val corner = h * 0.10f

            // Gradient background matching SVG
            drawRoundRect(
                brush = Brush.linearGradient(
                    colors = listOf(LogoTeal, LogoMid, LogoDeep),
                    start = Offset(0f, 0f),
                    end = Offset(w, h),
                ),
                cornerRadius = CornerRadius(corner, corner),
            )

            // Top flowing accent curve (SVG: M 80 180 Q 120 100 180 140 T 260 100)
            val topCurve = Path().apply {
                moveTo(w * 0.12f, h * 0.52f)
                quadraticTo(w * 0.22f, h * 0.12f, w * 0.38f, h * 0.32f)
                quadraticTo(w * 0.52f, h * 0.52f, w * 0.62f, h * 0.16f)
            }
            drawPath(
                path = topCurve,
                color = Color.White.copy(alpha = 0.40f),
                style = Stroke(width = w * 0.020f, cap = StrokeCap.Round),
            )

            // Bottom flowing accent curve (SVG: M 120 380 Q 250 460 380 360 T 450 420)
            val botCurve = Path().apply {
                moveTo(w * 0.16f, h * 0.88f)
                quadraticTo(w * 0.50f, h * 1.10f, w * 0.80f, h * 0.72f)
                quadraticTo(w * 0.95f, h * 0.58f, w * 0.98f, h * 0.92f)
            }
            drawPath(
                path = botCurve,
                color = Color.White.copy(alpha = 0.50f),
                style = Stroke(width = w * 0.028f, cap = StrokeCap.Round),
            )

            // 漫 character – the core SVG element
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint().apply {
                    color = android.graphics.Color.WHITE
                    textSize = h * 0.66f
                    typeface = Typeface.create("sans-serif", Typeface.BOLD)
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                    setShadowLayer(h * 0.05f, 0f, h * 0.028f, android.graphics.Color.argb(70, 0, 0, 0))
                    alpha = 230
                }
                canvas.nativeCanvas.drawText("漫", w * 0.5f, h * 0.73f, paint)
            }
        }

        HorizontalDivider()
    }
}
