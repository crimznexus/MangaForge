package eu.kanade.tachiyomi.ui.suggestions

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import tachiyomi.presentation.core.components.material.Scaffold

object SuggestionsScreen : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SuggestionsScreenModel() }
        val state by screenModel.state.collectAsState()

        // Determine visible rows based on active tab and genre filters
        val visibleRowIds = when (state.selectedTab) {
            0 -> {
                val all = state.rows.filter { it.id.startsWith("manga_") }.map { it.id }.toSet()
                if (state.activeGenreFilters.isEmpty()) all
                else setOf("manga_trending") + state.activeGenreFilters.map { "manga_genre_$it" }
            }
            else -> {
                val all = state.rows.filter { it.id.startsWith("manhwa_") }.map { it.id }.toSet()
                if (state.activeGenreFilters.isEmpty()) all
                else setOf("manhwa_trending_kr", "manhwa_trending_cn") +
                    state.activeGenreFilters.map { "manhwa_genre_$it" }
            }
        }
        val visibleRows = state.rows.filter { it.id in visibleRowIds }

        Scaffold(
            topBar = {
                Column {
                    TopAppBar(
                        title = { Text("Suggestions") },
                        actions = {
                            IconButton(onClick = screenModel::refresh) {
                                Icon(Icons.Outlined.Refresh, contentDescription = "Refresh")
                            }
                        },
                    )
                    PrimaryTabRow(selectedTabIndex = state.selectedTab) {
                        Tab(
                            selected = state.selectedTab == 0,
                            onClick = { screenModel.selectTab(0) },
                            text = { Text("Manga") },
                        )
                        Tab(
                            selected = state.selectedTab == 1,
                            onClick = { screenModel.selectTab(1) },
                            text = { Text("Manhwa") },
                        )
                    }
                }
            },
        ) { paddingValues ->
            if (state.genresLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = paddingValues,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(key = "genre_filter_section") {
                        GenreFilterSection(
                            genres = state.availableGenres,
                            activeFilters = state.activeGenreFilters,
                            expanded = state.filtersExpanded,
                            onToggleExpanded = screenModel::toggleFiltersExpanded,
                            onToggleFilter = screenModel::toggleGenreFilter,
                        )
                        HorizontalDivider()
                    }

                    visibleRows.forEach { row ->
                        item(key = row.id + "_header") {
                            SectionHeader(title = row.displayTitle)
                        }
                        item(key = row.id + "_content") {
                            when (val s = row.state) {
                                is RowItemState.Loading -> LoadingRow()
                                is RowItemState.Error -> ErrorRow(message = s.message)
                                is RowItemState.Success -> {
                                    if (s.items.isEmpty()) {
                                        EmptyRow()
                                    } else {
                                        SuggestionCardRow(
                                            items = s.items,
                                            onClickItem = { item ->
                                                navigator.push(GlobalSearchScreen(searchQuery = item.title))
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

// ── Filter section ────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenreFilterSection(
    genres: List<String>,
    activeFilters: Set<String>,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onToggleFilter: (String) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpanded)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (activeFilters.isEmpty()) {
                    "Filter by Genre"
                } else {
                    "Filter by Genre  •  ${activeFilters.size} selected"
                },
                style = MaterialTheme.typography.labelLarge,
                color = if (activeFilters.isNotEmpty()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Icon(
                imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                genres.forEach { genre ->
                    FilterChip(
                        selected = genre in activeFilters,
                        onClick = { onToggleFilter(genre) },
                        label = { Text(genre) },
                    )
                }
            }
        }
    }
}

// ── Section components ────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun SuggestionCardRow(
    items: List<AnilistSuggestionItem>,
    onClickItem: (AnilistSuggestionItem) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(items, key = { it.id }) { item ->
            SuggestionCard(item = item, onClick = { onClickItem(item) })
        }
    }
}

@Composable
private fun SuggestionCard(
    item: AnilistSuggestionItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(96.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            MangaCover.Book(
                data = item.coverUrl,
                modifier = Modifier.fillMaxWidth(),
                contentDescription = item.title,
            )
            val score = item.score
            if (score != null && score > 0) {
                Badge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = "%.1f".format(score / 10.0),
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun LoadingRow() {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(6) {
            Box(
                modifier = Modifier
                    .width(96.dp)
                    .aspectRatio(2f / 3f)
                    .clip(MaterialTheme.shapes.extraSmall),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.width(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ErrorRow(message: String) {
    Text(
        text = "Could not load: $message",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun EmptyRow() {
    Text(
        text = "No results",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
