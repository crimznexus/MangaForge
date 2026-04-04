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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.animation.core.AnimationEndReason
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
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
import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

// ── Score colours (functional, not brand) ────────────────────────────────────

private val BrandGreen = Color(0xFF00C853)
private val BrandAmber = Color(0xFFFFAB00)

// ── Screen ────────────────────────────────────────────────────────────────────

object SuggestionsScreen : Screen() {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel { SuggestionsScreenModel() }
        val state by screenModel.state.collectAsState()
        val scope = rememberCoroutineScope()

        // Pager is the source of truth for the active tab
        val pagerState = rememberPagerState(pageCount = { 2 })
        // Disable pager swipe while the hero carousel is being dragged
        var isCarouselDragging by remember { mutableStateOf(false) }

        // Keep ScreenModel in sync so it loads data for the right tab
        LaunchedEffect(pagerState.currentPage) {
            screenModel.selectTab(pagerState.currentPage)
        }

        val onClickItem: (AnilistSuggestionItem) -> Unit = { item ->
            navigator.push(
                MangaDetailScreen(
                    itemId = item.id,
                    itemTitle = item.title,
                    itemCoverUrl = item.coverUrl,
                    itemScore = item.score,
                    itemFormat = item.format,
                ),
            )
        }

        Scaffold(
            topBar = {
                GradientHeader(
                    selectedTab   = pagerState.currentPage,
                    onSelectTab   = { scope.launch { pagerState.animateScrollToPage(it) } },
                    onOpenFilters = screenModel::openFilters,
                    onSearch      = { navigator.push(GlobalSearchScreen(searchQuery = "")) },
                )
            },
        ) { paddingValues ->
            if (state.genresLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
            } else {
                // Tab swipe — each page is an independent scroll container
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    userScrollEnabled = !isCarouselDragging,
                    beyondViewportPageCount = 1,
                ) { page ->
                    val featuredState = if (page == 0) state.mangaFeatured  else state.manhwaFeatured
                    val trendingState = if (page == 0) state.mangaTrending  else state.manhwaTrending
                    val popularState  = if (page == 0) state.mangaPopular   else state.manhwaPopular
                    val newState      = if (page == 0) state.mangaNew       else state.manhwaNew
                    val topRatedState = if (page == 0) state.mangaTopRated  else state.manhwaTopRated
                    val resultsState  = if (page == 0) state.mangaResults   else state.manhwaResults
                    val applied       = if (page == 0) state.appliedManga   else state.appliedManhwa

                    var trendingOrPopular by remember { mutableStateOf(0) }

                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = screenModel::refresh,
                        modifier = Modifier.fillMaxSize(),
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
                                onDraggingChanged = { isCarouselDragging = it },
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
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text("Filters", color = MaterialTheme.colorScheme.primary)
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
                                ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
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
                }   // end HorizontalPager page
            }
        }

        if (state.filtersSheetOpen) {
            val draft = if (pagerState.currentPage == 0) state.draftManga else state.draftManhwa
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
    onSearch: () -> Unit,
) {
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val primary = MaterialTheme.colorScheme.primary
    val headerGradient = listOf(primaryContainer, primary)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Brush.linearGradient(headerGradient)),
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
                Row {
                    IconButton(onClick = onSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = "Search", tint = Color.White)
                    }
                    IconButton(onClick = onOpenFilters) {
                        Icon(Icons.Outlined.Tune, contentDescription = "Filters", tint = Color.White)
                    }
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
                            color = if (selected) primary else Color.White,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }
        }
    }
}

// ── Hero stacked carousel ─────────────────────────────────────────────────────
//
// Custom draw-order carousel: prev + next drawn first, current drawn last so it
// always paints on top — no HorizontalPager z-ordering hacks needed.

@Composable
private fun HeroBanner(
    state: ResultsState,
    onClickItem: (AnilistSuggestionItem) -> Unit,
    onDraggingChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val items = (state as? ResultsState.Success)?.items?.take(10) ?: emptyList()

    if (items.isEmpty()) {
        if (state is ResultsState.Loading) {
            Box(
                modifier = modifier.fillMaxWidth().height(300.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator(color = MaterialTheme.colorScheme.primary) }
        }
        return
    }

    var currentIdx by remember { mutableStateOf(0) }
    val scrollPos = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    // Width captured via onSizeChanged so pointerInput can live on the outer modifier
    var containerWidthPx by remember { mutableStateOf(1f) }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(4_000)
            if (!isDragging) {
                val r = scrollPos.animateTo(-1f, tween(420, easing = FastOutSlowInEasing))
                if (r.endReason == AnimationEndReason.Finished) {
                    currentIdx = (currentIdx + 1) % items.size
                    scrollPos.snapTo(0f)
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
            .clipToBounds()   // clip overflow so side cards don't bleed into adjacent pager page
            .onSizeChanged { containerWidthPx = it.width.toFloat() }
            .pointerInput(items.size) {
                detectHorizontalDragGestures(
                    onDragStart = {
                        isDragging = true
                        onDraggingChanged(true)
                    },
                    onDragCancel = {
                        isDragging = false
                        onDraggingChanged(false)
                    },
                    onDragEnd = {
                        isDragging = false
                        onDraggingChanged(false)
                        val v = scrollPos.value
                        scope.launch {
                            when {
                                v < -0.20f -> {
                                    val r = scrollPos.animateTo(-1f, tween(280, easing = FastOutSlowInEasing))
                                    if (r.endReason == AnimationEndReason.Finished) {
                                        currentIdx = (currentIdx + 1) % items.size
                                        scrollPos.snapTo(0f)
                                    }
                                }
                                v > 0.20f -> {
                                    val r = scrollPos.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                                    if (r.endReason == AnimationEndReason.Finished) {
                                        currentIdx = (currentIdx - 1 + items.size) % items.size
                                        scrollPos.snapTo(0f)
                                    }
                                }
                                else -> scrollPos.animateTo(0f, spring(dampingRatio = 0.72f, stiffness = 380f))
                            }
                        }
                    },
                    onHorizontalDrag = { _, delta ->
                        val next = (scrollPos.value + delta / containerWidthPx).coerceIn(-1f, 1f)
                        scope.launch { scrollPos.snapTo(next) }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {

        val cardWidth = maxWidth * 0.62f
        // spacing between card centres — tight enough that side cards peek from behind centre
        val spacing = maxWidth * 0.50f
        val pos = scrollPos.value

        // Each card's normalised offset from screen centre (0 = centre, ±1 = ±spacing)
        val prevNorm = -1f + pos   // pos=+1 → 0 (prev comes to centre)
        val currNorm = pos         // pos=0  → 0 (resting at centre)
        val nextNorm = 1f + pos    // pos=-1 → 0 (next comes to centre)

        fun scale(n: Float) = lerp(0.76f, 1.00f, 1f - abs(n).coerceIn(0f, 1f))
        fun alpha(n: Float) = lerp(0.48f, 1.00f, 1f - abs(n).coerceIn(0f, 1f))
        fun transY(n: Float) = lerp(22f, 0f, 1f - abs(n).coerceIn(0f, 1f)).dp
        // Fan tilt: left card leans right (+), right card leans left (–)
        fun rotZ(n: Float) = n.coerceIn(-1f, 1f) * (-4f)

        val prevIdx = (currentIdx - 1 + items.size) % items.size
        val nextIdx = (currentIdx + 1) % items.size

        // Draw prev and next FIRST so current paints on top
        CarouselCard(
            item = items[prevIdx],
            cardWidth = cardWidth,
            offsetX = spacing * prevNorm,
            scale = scale(prevNorm),
            alpha = alpha(prevNorm),
            transY = transY(prevNorm),
            rotZ = rotZ(prevNorm),
            isFront = false,
            onClick = {},
        )
        CarouselCard(
            item = items[nextIdx],
            cardWidth = cardWidth,
            offsetX = spacing * nextNorm,
            scale = scale(nextNorm),
            alpha = alpha(nextNorm),
            transY = transY(nextNorm),
            rotZ = rotZ(nextNorm),
            isFront = false,
            onClick = {},
        )
        // Current card — drawn last, always on top
        CarouselCard(
            item = items[currentIdx],
            cardWidth = cardWidth,
            offsetX = spacing * currNorm,
            scale = scale(currNorm),
            alpha = alpha(currNorm),
            transY = transY(currNorm),
            rotZ = rotZ(currNorm),
            isFront = true,
            onClick = { if (abs(pos) < 0.08f) onClickItem(items[currentIdx]) },
        )

        // Page dots
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(items.size) { i ->
                val sel = i == currentIdx
                Box(
                    modifier = Modifier
                        .size(if (sel) 7.dp else 4.dp)
                        .clip(CircleShape)
                        .background(if (sel) Color.White else Color.White.copy(alpha = 0.38f)),
                )
            }
        }
    }
}

@Composable
private fun CarouselCard(
    item: AnilistSuggestionItem,
    cardWidth: Dp,
    offsetX: Dp,
    scale: Float,
    alpha: Float,
    transY: Dp,
    rotZ: Float,
    isFront: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(cardWidth)
            .fillMaxHeight(0.91f)
            .graphicsLayer {
                translationX = offsetX.toPx()
                translationY = transY.toPx()
                scaleX = scale
                scaleY = scale
                this.alpha = alpha
                rotationZ = rotZ
            },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isFront) 18.dp else 4.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (isFront) {
                // Gradient scrim + title text on the front card only
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                0.52f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.88f),
                            ),
                        ),
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 14.dp, end = 14.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    item.format?.let { fmt ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.88f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = fmt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                            )
                        }
                    }
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    item.score?.takeIf { it > 0 }?.let { score ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            val starColor = when {
                                score >= 80 -> BrandGreen
                                score >= 60 -> BrandAmber
                                else -> Color.White
                            }
                            Text("★", color = starColor, style = MaterialTheme.typography.labelSmall)
                            Text(
                                text = "%.1f".format(score / 10.0),
                                color = Color.White.copy(alpha = 0.90f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            } else {
                // Score badge on side cards
                item.score?.takeIf { it > 0 }?.let { score ->
                    Badge(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                        containerColor = when {
                            score >= 80 -> BrandGreen
                            score >= 60 -> BrandAmber
                            else -> MaterialTheme.colorScheme.primaryContainer
                        },
                        contentColor = Color.White,
                    ) {
                        Text("%.1f".format(score / 10.0), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ── Section header with gradient accent bar ───────────────────────────────────

@Composable
private fun ContentSectionHeader(title: String, modifier: Modifier = Modifier) {
    val accentGradient = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
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
                .background(Brush.verticalGradient(accentGradient)),
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
    val selectedGradient = listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
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
                            Modifier.background(Brush.linearGradient(selectedGradient))
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
        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) }
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
        ) { CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.primary) }
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
        val currentYear = remember { Year.now().value }
        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item {
                Text(
                    text = "Filters",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }

            item {
                Column {
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
                }
            }

            item {
                Column {
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
                }
            }

            item {
                Column {
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
                }
            }

            item {
                Column {
                    SectionHeader(title = "Year")
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
                }
            }

            item {
                Column {
                    SectionHeader(
                        title = if (draft.genres.isEmpty()) "Genres" else "Genres (${draft.genres.size})",
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(0.dp),
                    ) {
                        availableGenres.forEach { genre ->
                            FilterChip(
                                selected = genre in draft.genres,
                                onClick = { onToggleGenre(genre) },
                                label = { Text(genre) },
                            )
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onReset) { Text("Reset") }
                    Button(onClick = onApply) { Text("Apply") }
                }
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
