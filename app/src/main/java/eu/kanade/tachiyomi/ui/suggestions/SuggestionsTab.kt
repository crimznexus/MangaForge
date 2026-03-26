package eu.kanade.tachiyomi.ui.suggestions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab

data object SuggestionsTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = "Discover",
            icon = rememberVectorPainter(Icons.Outlined.AutoAwesome),
        )

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(SuggestionsScreen)
    }

    @Composable
    override fun Content() {
        Navigator(SuggestionsScreen)
    }
}
