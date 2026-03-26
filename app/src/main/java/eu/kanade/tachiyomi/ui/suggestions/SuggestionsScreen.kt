package eu.kanade.tachiyomi.ui.suggestions

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import tachiyomi.presentation.core.components.material.Scaffold
import java.time.Year

// ── Brand colour palette ──────────────────────────────────────────────────────

private val BrandDeep    = Color(0xFF3A0075)
private val BrandPurple  = Color(0xFF7B2FBE)
private val BrandViolet  = Color(0xFFCC44FF)
private val BrandCoral   = Color(0xFFFF6B6B)
private val BrandGreen   = Color(0xFF00C853)
private val BrandAmber   = Color(0xFFFFAB00)

private val HeaderGradient   = listOf(BrandDeep, BrandPurple, BrandViolet)
private val AccentGradient   = listOf(BrandPurple, BrandCoral)
private val SelectedGradient = listOf(BrandPurple, BrandViolet)

// ── Screen ────────────────────────────────────────────────────────────────────

object SuggestionsScreen : Screen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SuggestionsScreenModel() }
        val state by screenModel.state.collectAsState()

        val featuredState = if (state.selectedTab == 0) state.mangaFeatured  else state.manhwaFeatured
        val trendingState = if (state.selectedTab == 0) state.mangaTrending  else state.manhwaTrending
        val popularState  = if (state.selectedTab == 0) state.mangaPopular   else state.manhwaPopular
        val newState      = if (state.selectedTab == 0) state.mangaNew       else state.manhwaNew
        val topRatedState = if (state.selectedTab == 0) state.mangaTopRated  else state.manhwaTopRated
        val resultsState  = if (state.selectedTab == 0) state.mangaResults   else state.manhwaResults
        val applied       = if (state.selectedTab == 0) state.appliedManga   else state.appliedManhwa
        val draft         = if (state.selectedTab == 0) state.draftManga     else state.draftManhwa

        var trendingOrPopular by remember(state.selectedTab) { mutableStateOf(0) }

        val onClickItem: (AnilistSuggestionItem) -> Unit = { item ->
            navigator.push(GlobalSearchScreen(searchQuery = item.title))
        }

        Scaffold(
            topBar = {
                GradientHeader(
                    selectedTab  = state.selectedTab,
                    onSelectTab  = screenModel::selectTab,
                    onOpenFilters = screenModel::openFilters,
                )
            },
        ) { paddingValues ->
            if (state.genresLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = BrandPurple) }
            } else {
                PullToRefreshBox(
                    isRefreshing = state.isRefreshing,
                    onRefresh = screenModel::refresh,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                ) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 110.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // ── Hero ─────────────────────────────────────────────
                    item(key = "hero", span = { GridItemSpan(maxLineSpan) }) {
                        HeroBanner(
                            state = featuredState,
                            onClickItem = onClickItem,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }

                    // ── Trending & Popular ────────────────────────────────
                    item(key = "trending_section", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            ContentSectionHeader(
                                title = "Trending & Popular",
                                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                            )
                            PillToggle(
                                options  = listOf("Trending", "Popular"),
                                selected = trendingOrPopular,
                                onSelect = { trendingOrPopular = it },
                            )
                            Spacer(Modifier.height(12.dp))
                            RankedCoverRow(
                                state = if (trendingOrPopular == 0) trendingState else popularState,
                                onClickItem = onClickItem,
                            )
                        }
                    }

                    // ── Newly Released ────────────────────────────────────
                    item(key = "new_section", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            ContentSectionHeader(
                                title = "Newly Released",
                                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                            )
                            HorizontalCoverRow(state = newState, onClickItem = onClickItem)
                        }
                    }

                    // ── Top Rated ─────────────────────────────────────────
                    item(key = "toprated_section", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            ContentSectionHeader(
                                title = "Top Rated",
                                modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
                            )
                            HorizontalCoverRow(state = topRatedState, onClickItem = onClickItem)
                        }
                    }

                    // ── Browse ────────────────────────────────────────────
                    item(key = "browse_header", span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ContentSectionHeader(title = "Browse")
                                TextButton(onClick = screenModel::openFilters) {
                                    Icon(
                                        Icons.Outlined.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = BrandPurple,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Filters", color = BrandPurple)
                                }
                            }
                            AppliedFilterChipsRow(
                                applied = applied,
                                onOpenFilters = screenModel::openFilters,
                                modifier = Modifier.padding(vertical = 4.dp),
                            )
                            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
                        }
                    }

                    when (resultsState) {
                        is ResultsState.Loading -> item(
                            key = "browse_loading",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center,
                            ) { CircularProgressIndicator(color = BrandPurple) }
                        }
                        is ResultsState.Error -> item(
                            key = "browse_error",
                            span = { GridItemSpan(maxLineSpan) },
                        ) { ErrorRow(message = resultsState.message) }
                        is ResultsState.Success -> {
                            if (resultsState.items.isEmpty()) {
                                item(key = "browse_empty", span = { GridItemSpan(maxLineSpan) }) {
                                    EmptyRow()
                                }
                            } else {
                                items(resultsState.items, key = { it.id }) { item ->
                                    SuggestionCard(item = item, onClick = { onClickItem(item) })
                                }
                            }
                        }
                    }
                }
                } // end PullToRefreshBox
            }
        }

        if (state.filtersSheetOpen) {
            FiltersBottomSheet(
                draft          = draft,
                availableGenres = state.availableGenres,
                onDismiss      = screenModel::closeFilters,
                onReset        = screenModel::resetDraft,
                onApply        = screenModel::applyDraft,
                onSelectSort   = screenModel::updateDraftSort,
                onSelectStatus = screenModel::updateDraftStatus,
                onSelectFormat = screenModel::updateDraftFormat,
                onSelectYear   = screenModel::updateDraftYear,
                onToggleGenre  = screenModel::toggleDraftGenre,
            )
        }
    }
}

// ── Gradient header ───────────────────────────────────────────────────────────

@Composable
private fun GradientHeader(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onOpenFilters: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(HeaderGradient)),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 18.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column {
                    Text(
                        text = "Discover",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Text(
                        text = "Find your next read",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
                IconButton(onClick = onOpenFilters) {
                    Icon(
                        Icons.Outlined.Tune,
                        contentDescription = "Filters",
                        tint = Color.White,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            // Manga / Manhwa pill switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(4.dp),
            ) {
                listOf("Manga", "Manhwa").forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .then(if (selected) Modifier.background(Color.White) else Modifier)
                            .clickable { onSelectTab(index) }
                            .padding(horizontal = 28.dp, vertical = 9.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            color = if (selected) BrandPurple else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

// ── Hero banner ───────────────────────────────────────────────────────────────

@Composable
private fun HeroBanner(
    state: ResultsState,
    onClickItem: (AnilistSuggestionItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = (state as? ResultsState.Success)?.items?.firstOrNull()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(320.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(if (item != null) Modifier.clickable { onClickItem(item) } else Modifier),
    ) {
        if (item != null) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.28f to Color.Transparent,
                            1.0f to Color.Black.copy(alpha = 0.92f),
                        ),
                    ),
            )
            // Coloured accent strip at the bottom
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Brush.linearGradient(AccentGradient)),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            ) {
                // Format badge
                item.format?.let { fmt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(BrandPurple.copy(alpha = 0.88f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                    ) {
                        Text(
                            text = fmt,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onClickItem(item) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = BrandPurple,
                    ),
                ) {
                    Text("Start Reading", fontWeight = FontWeight.Bold)
                }
            }
        } else if (state is ResultsState.Loading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = BrandPurple,
            )
        }
    }
}

// ── Section header with gradient accent bar ───────────────────────────────────

@Composable
private fun ContentSectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.verticalGradient(AccentGradient)),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

// ── Pill toggle (Trending / Popular) ─────────────────────────────────────────

@Composable
private fun PillToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = selected == index
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .then(
                        if (isSelected) {
                            Modifier.background(Brush.linearGradient(SelectedGradient))
                        } else {
                            Modifier
                        },
                    )
                    .clickable { onSelect(index) }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}

// ── Ranked row (Trending / Popular) ──────────────────────────────────────────

@Composable
private fun RankedCoverRow(
    state: ResultsState,
    onClickItem: (AnilistSuggestionItem) -> Unit,
) {
    when (state) {
        is ResultsState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().height(170.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandPurple) }
        is ResultsState.Error -> {}
        is ResultsState.Success -> LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.items.take(20), key = { _, it -> it.id }) { index, item ->
                RankedCoverCard(item = item, rank = index + 1, onClick = { onClickItem(item) })
            }
        }
    }
}

@Composable
private fun RankedCoverCard(
    item: AnilistSuggestionItem,
    rank: Int,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.width(100.dp).clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            MangaCover.Book(
                data = item.coverUrl,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentDescription = item.title,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f)),
                        ),
                    )
                    .padding(start = 6.dp, top = 20.dp, bottom = 4.dp),
            ) {
                Text(
                    text = rank.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
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

// ── Standard horizontal cover row ────────────────────────────────────────────

@Composable
private fun HorizontalCoverRow(
    state: ResultsState,
    onClickItem: (AnilistSuggestionItem) -> Unit,
) {
    when (state) {
        is ResultsState.Loading -> Box(
            modifier = Modifier.fillMaxWidth().height(170.dp),
            contentAlignment = Alignment.Center,
        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = BrandPurple) }
        is ResultsState.Error -> {}
        is ResultsState.Success -> LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            itemsIndexed(state.items.take(20), key = { _, it -> it.id }) { _, item ->
                CoverCard(item = item, onClick = { onClickItem(item) })
            }
        }
    }
}

@Composable
private fun CoverCard(item: AnilistSuggestionItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(100.dp).clickable(onClick = onClick),
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f)) {
            MangaCover.Book(
                data = item.coverUrl,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentDescription = item.title,
            )
            val score = item.score
            if (score != null && score > 0) {
                val badgeColor = when {
                    score >= 80 -> BrandGreen
                    score >= 60 -> BrandAmber
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    containerColor = badgeColor,
                    contentColor = Color.White,
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

// ── Browse grid card ──────────────────────────────────────────────────────────

@Composable
private fun SuggestionCard(
    item: AnilistSuggestionItem,
    onClick: () -> Unit,
) {
    Column(
        modifier = Modifier.clickable(onClick = onClick).wrapContentHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            MangaCover.Book(
                data = item.coverUrl,
                modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
                shape = MaterialTheme.shapes.medium,
                contentDescription = item.title,
            )
            val score = item.score
            if (score != null && score > 0) {
                val badgeColor = when {
                    score >= 80 -> BrandGreen
                    score >= 60 -> BrandAmber
                    else -> MaterialTheme.colorScheme.primaryContainer
                }
                Badge(
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                    containerColor = badgeColor,
                    contentColor = Color.White,
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
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

// ── Applied filter chips row ──────────────────────────────────────────────────

@Composable
private fun AppliedFilterChipsRow(
    applied: Filters,
    onOpenFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = false,
                onClick = onOpenFilters,
                label = { Text(applied.sort.title) },
            )
        }
        item {
            val label = if (applied.genres.isEmpty()) "Genres" else "Genres (${applied.genres.size})"
            FilterChip(selected = applied.genres.isNotEmpty(), onClick = onOpenFilters, label = { Text(label) })
        }
        item {
            FilterChip(
                selected = applied.status != null,
                onClick = onOpenFilters,
                label = { Text(applied.status?.title ?: "Status") },
            )
        }
        item {
            FilterChip(
                selected = applied.format != null,
                onClick = onOpenFilters,
                label = { Text(applied.format?.title ?: "Format") },
            )
        }
        item {
            FilterChip(
                selected = applied.year != null,
                onClick = onOpenFilters,
                label = { Text(applied.year?.toString() ?: "Year") },
            )
        }
    }
}

// ── Filters bottom sheet ──────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FiltersBottomSheet(
    draft: Filters,
    availableGenres: List<String>,
    onDismiss: () -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
    onSelectSort: (SortOption) -> Unit,
    onSelectStatus: (StatusOption?) -> Unit,
    onSelectFormat: (FormatOption?) -> Unit,
    onSelectYear: (Int?) -> Unit,
    onToggleGenre: (String) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Filters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SectionHeader(title = "Sort")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortOption.entries.forEach { option ->
                    FilterChip(
                        selected = draft.sort == option,
                        onClick = { onSelectSort(option) },
                        label = { Text(option.title) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionHeader(title = "Status")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = draft.status == null, onClick = { onSelectStatus(null) }, label = { Text("Any") })
                StatusOption.entries.forEach { option ->
                    FilterChip(
                        selected = draft.status == option,
                        onClick = { onSelectStatus(option) },
                        label = { Text(option.title) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionHeader(title = "Format")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = draft.format == null, onClick = { onSelectFormat(null) }, label = { Text("Any") })
                FormatOption.entries.forEach { option ->
                    FilterChip(
                        selected = draft.format == option,
                        onClick = { onSelectFormat(option) },
                        label = { Text(option.title) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionHeader(title = "Year")
            val currentYear = remember { Year.now().value }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(selected = draft.year == null, onClick = { onSelectYear(null) }, label = { Text("Any") })
                }
                itemsIndexed((0..10).toList()) { _, delta ->
                    val year = currentYear - delta
                    FilterChip(
                        selected = draft.year == year,
                        onClick = { onSelectYear(year) },
                        label = { Text(year.toString()) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            SectionHeader(
                title = if (draft.genres.isEmpty()) "Genres" else "Genres (${draft.genres.size})",
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                availableGenres.forEach { genre ->
                    FilterChip(
                        selected = genre in draft.genres,
                        onClick = { onToggleGenre(genre) },
                        label = { Text(genre) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onReset) { Text("Reset") }
                Button(onClick = onApply) { Text("Apply") }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

// ── Error / empty ─────────────────────────────────────────────────────────────

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
