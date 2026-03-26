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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.WindowInsets
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

// Brand colours (shared palette)
private val DetailBrandDeep   = Color(0xFF3A0075)
private val DetailBrandPurple = Color(0xFF7B2FBE)
private val DetailBrandViolet = Color(0xFFCC44FF)
private val DetailBrandGreen  = Color(0xFF00C853)
private val DetailBrandAmber  = Color(0xFFFFAB00)

// ── Screen ────────────────────────────────────────────────────────────────────

data class MangaDetailScreen(
    private val itemId: Int,
    private val itemTitle: String,
    private val itemCoverUrl: String,
    private val itemScore: Int?,
    private val itemFormat: String?,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val screenModel = rememberScreenModel {
            MangaDetailScreenModel(
                AnilistSuggestionItem(
                    id = itemId,
                    title = itemTitle,
                    coverUrl = itemCoverUrl,
                    score = itemScore,
                    format = itemFormat,
                ),
            )
        }
        val state by screenModel.state.collectAsState()

        Scaffold(
            contentWindowInsets = WindowInsets(0),
            bottomBar = {
                Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Button(
                            onClick = { navigator.push(GlobalSearchScreen(searchQuery = state.title)) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DetailBrandPurple,
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Search in Extensions", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = padding.calculateBottomPadding()),
            ) {
                item(key = "hero") {
                    DetailHero(
                        title = state.title,
                        coverUrl = state.coverUrl,
                        bannerUrl = state.bannerUrl,
                        score = state.score,
                        format = state.format,
                        status = state.status,
                        isLoading = state.isLoading,
                        onBack = { navigator.pop() },
                    )
                }

                if (state.genres.isNotEmpty()) {
                    item(key = "genres") { GenresSection(genres = state.genres) }
                }

                val hasStats = state.year != null || state.chapters != null || state.volumes != null
                if (hasStats) {
                    item(key = "stats") {
                        StatsSection(
                            year = state.year,
                            chapters = state.chapters,
                            volumes = state.volumes,
                        )
                    }
                }

                state.description?.takeIf { it.isNotBlank() }?.let { desc ->
                    item(key = "description") { DescriptionSection(description = desc) }
                }

                if (state.recommendations.isNotEmpty()) {
                    item(key = "related_header") {
                        Text(
                            text = "You Might Also Like",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 10.dp),
                        )
                    }
                    item(key = "related_row") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(state.recommendations, key = { it.id }) { rec ->
                                RelatedCard(
                                    item = rec,
                                    onClick = {
                                        navigator.push(
                                            MangaDetailScreen(
                                                itemId = rec.id,
                                                itemTitle = rec.title,
                                                itemCoverUrl = rec.coverUrl,
                                                itemScore = rec.score,
                                                itemFormat = rec.format,
                                            ),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                item(key = "bottom_spacer") { Spacer(Modifier.height(20.dp)) }
            }
        }
    }
}

// ── Hero ──────────────────────────────────────────────────────────────────────

@Composable
private fun DetailHero(
    title: String,
    coverUrl: String,
    bannerUrl: String?,
    score: Int?,
    format: String?,
    status: String?,
    isLoading: Boolean,
    onBack: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth().height(340.dp)) {
        // Banner or cover as background
        AsyncImage(
            model = bannerUrl ?: coverUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Dark gradient overlay
        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0.0f to Color.Black.copy(alpha = 0.35f),
                    0.45f to Color.Transparent,
                    1.0f to Color.Black.copy(alpha = 0.95f),
                ),
            ),
        )

        // Back button
        IconButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.TopStart).statusBarsPadding().padding(4.dp),
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        // Loading spinner over hero while fetching
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(36.dp),
                color = DetailBrandViolet,
                strokeWidth = 3.dp,
            )
        }

        // Cover card + title row at bottom
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 20.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                shape = RoundedCornerShape(8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.width(100.dp).height(150.dp),
            ) {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            Column(
                modifier = Modifier.weight(1f).padding(bottom = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                format?.let { fmt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(DetailBrandPurple.copy(alpha = 0.88f))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
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
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (score != null && score > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        val starColor = when {
                            score >= 80 -> DetailBrandGreen
                            score >= 60 -> DetailBrandAmber
                            else -> Color.White
                        }
                        Text("★", color = starColor, style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = "%.1f".format(score / 10.0),
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                status?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                }
            }
        }

        // Bottom accent strip
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(3.dp)
                .background(Brush.linearGradient(listOf(DetailBrandPurple, DetailBrandViolet))),
        )
    }
}

// ── Genres ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GenresSection(genres: List<String>) {
    FlowRow(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        genres.forEach { genre ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(DetailBrandPurple.copy(alpha = 0.15f))
                    .padding(horizontal = 12.dp, vertical = 5.dp),
            ) {
                Text(
                    text = genre,
                    style = MaterialTheme.typography.labelMedium,
                    color = DetailBrandViolet,
                )
            }
        }
    }
}

// ── Stats ─────────────────────────────────────────────────────────────────────

@Composable
private fun StatsSection(year: Int?, chapters: Int?, volumes: Int?) {
    val stats = buildList {
        year?.let { add("Year: $it") }
        chapters?.let { add("$it chapters") }
        if (chapters == null) volumes?.let { add("$it volumes") }
    }
    if (stats.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp),
    ) {
        items(stats) { stat ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    text = stat,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ── Description ───────────────────────────────────────────────────────────────

@Composable
private fun DescriptionSection(description: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(
            text = "Synopsis",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = if (expanded) Int.MAX_VALUE else 4,
            overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
        TextButton(
            onClick = { expanded = !expanded },
            contentPadding = PaddingValues(0.dp),
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = if (expanded) "Show less" else "Read more",
                color = DetailBrandPurple,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// ── Related card ──────────────────────────────────────────────────────────────

@Composable
private fun RelatedCard(item: AnilistSuggestionItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(90.dp).clickable(onClick = onClick),
    ) {
        MangaCover.Book(
            data = item.coverUrl,
            modifier = Modifier.fillMaxWidth().aspectRatio(2f / 3f),
            shape = MaterialTheme.shapes.medium,
            contentDescription = item.title,
        )
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}
