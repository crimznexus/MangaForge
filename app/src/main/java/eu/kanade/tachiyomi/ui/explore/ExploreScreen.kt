package eu.kanade.tachiyomi.ui.explore

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.core.util.ifSourcesLoaded
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

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Explore") },
                    actions = {
                        IconButton(onClick = screenModel::loadPopular) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                        }
                    },
                )
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
