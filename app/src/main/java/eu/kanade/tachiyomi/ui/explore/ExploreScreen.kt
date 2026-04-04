package eu.kanade.tachiyomi.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
import eu.kanade.tachiyomi.ui.explore.ExploreTabChips
import eu.kanade.presentation.browse.components.GlobalSearchCardRow
import eu.kanade.presentation.browse.components.GlobalSearchErrorResultItem
import eu.kanade.presentation.browse.components.GlobalSearchLoadingResultItem
import eu.kanade.presentation.browse.components.GlobalSearchResultItem
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.LocaleHelper
import tachiyomi.domain.manga.model.Manga
import tachiyomi.presentation.core.components.material.Scaffold

object ExploreScreen : Screen() {

    @Composable
    override fun Content() {
        if (!ifSourcesLoaded()) return

        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { ExploreScreenModel() }
        val state by screenModel.state.collectAsState()
        val headerGradient = listOf(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.primary,
        )

        Scaffold(
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(headerGradient)),
                ) {
                    Row(
                        modifier = Modifier
                            .statusBarsPadding()
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column {
                            Text(
                                text = "Explore",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                            )
                            Text(
                                text = "Popular from your sources",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White.copy(alpha = 0.70f),
                            )
                        }
                        IconButton(onClick = screenModel::loadPopular) {
                            Icon(
                                Icons.Outlined.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White,
                            )
                        }
                    }
                    ExploreTabChips()
                }
            },
        ) { paddingValues ->
            ExploreContent(
                items = state.items,
                contentPadding = paddingValues,
                getManga = screenModel::getManga,
                onClickSource = { source ->
                    navigator.push(BrowseSourceScreen(source.id, null))
                },
                onClickItem = { manga ->
                    navigator.push(MangaScreen(manga.id))
                },
                onLongClickItem = { manga ->
                    navigator.push(MangaScreen(manga.id))
                },
            )
        }
    }
}

@Composable
private fun ExploreContent(
    items: Map<CatalogueSource, SearchItemResult>,
    contentPadding: PaddingValues,
    getManga: @Composable (Manga) -> State<Manga>,
    onClickSource: (CatalogueSource) -> Unit,
    onClickItem: (Manga) -> Unit,
    onLongClickItem: (Manga) -> Unit,
) {
    LazyColumn(contentPadding = contentPadding) {
        items.forEach { (source, result) ->
            item(key = source.id) {
                GlobalSearchResultItem(
                    title = source.name,
                    subtitle = LocaleHelper.getLocalizedDisplayName(source.lang),
                    onClick = { onClickSource(source) },
                    modifier = Modifier.animateItem(),
                ) {
                    when (result) {
                        SearchItemResult.Loading -> GlobalSearchLoadingResultItem()
                        is SearchItemResult.Success -> GlobalSearchCardRow(
                            titles = result.result,
                            getManga = getManga,
                            onClick = onClickItem,
                            onLongClick = onLongClickItem,
                        )
                        is SearchItemResult.Error -> GlobalSearchErrorResultItem(
                            message = result.throwable.message,
                        )
                    }
                }
            }
        }
    }
}
