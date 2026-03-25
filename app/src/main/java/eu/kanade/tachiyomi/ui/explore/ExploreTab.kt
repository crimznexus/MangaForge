package eu.kanade.tachiyomi.ui.explore

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab

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
        Navigator(ExploreScreen)
    }
}
