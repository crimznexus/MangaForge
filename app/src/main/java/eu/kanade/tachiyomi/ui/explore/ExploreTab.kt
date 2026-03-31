package eu.kanade.tachiyomi.ui.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab

// Shared state so each screen can read & render the sub-tab chips in its own header
val LocalExploreSubTab = compositionLocalOf<Int?> { null }
val LocalSetExploreSubTab = compositionLocalOf<(Int) -> Unit> { {} }

/** Three sub-tab chips rendered inside each screen's existing gradient header. */
@Composable
fun ExploreTabChips(modifier: Modifier = Modifier) {
    val selectedTab = LocalExploreSubTab.current ?: return
    val setTab = LocalSetExploreSubTab.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf("Explore", "Updates", "History").forEachIndexed { index, label ->
            FilterChip(
                selected = selectedTab == index,
                onClick = { setTab(index) },
                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    selectedContainerColor = Color.White.copy(alpha = 0.22f),
                    labelColor = Color.White.copy(alpha = 0.65f),
                    selectedLabelColor = Color.White,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedTab == index,
                    selectedBorderColor = Color.White.copy(alpha = 0.55f),
                    borderColor = Color.White.copy(alpha = 0.28f),
                ),
            )
        }
    }
}

data object ExploreTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 5u,
            title = "Explore",
            icon = rememberVectorPainter(Icons.Outlined.Explore),
        )

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(ExploreScreen)
    }

    @Composable
    override fun Content() {
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        CompositionLocalProvider(
            LocalExploreSubTab provides selectedTab,
            LocalSetExploreSubTab provides { selectedTab = it },
        ) {
            when (selectedTab) {
                0 -> Navigator(ExploreScreen)
                1 -> UpdatesTab.Content()
                else -> HistoryTab.Content()
            }
        }
    }
}
