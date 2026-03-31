package eu.kanade.tachiyomi.ui.explore

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab

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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Explore") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Updates") },
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("History") },
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .consumeWindowInsets(WindowInsets.statusBars),
            ) {
                when (selectedTab) {
                    0 -> Navigator(ExploreScreen)
                    1 -> UpdatesTab.Content()
                    2 -> HistoryTab.Content()
                }
            }
        }
    }
}
