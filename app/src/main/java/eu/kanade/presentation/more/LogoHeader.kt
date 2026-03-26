package eu.kanade.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

private val Purple0 = Color(0xFF8477FF)
private val Purple1 = Color(0xFF5A18C0)

@Composable
fun LogoHeader() {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(Purple0, Purple1),
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = eu.kanade.tachiyomi.R.drawable.ic_mihon),
                contentDescription = null,
                modifier = Modifier.size(120.dp)
            )
        }

        HorizontalDivider()
    }
}
