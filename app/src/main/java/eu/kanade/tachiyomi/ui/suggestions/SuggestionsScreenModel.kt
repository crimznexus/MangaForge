package eu.kanade.tachiyomi.ui.suggestions

import androidx.compose.runtime.Immutable
import cafe.adriel.voyager.core.model.StateScreenModel
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.interceptor.rateLimit
import eu.kanade.tachiyomi.network.jsonMime
import eu.kanade.tachiyomi.network.parseAs
import okhttp3.Headers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.RequestBody.Companion.toRequestBody
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import uy.kohesive.injekt.injectLazy
import kotlin.time.Duration.Companion.minutes

// ── DTOs ─────────────────────────────────────────────────────────────────────

@Serializable data class ALSuggestionsResult(val data: ALSuggestionsData)
@Serializable data class ALSuggestionsData(@SerialName("Page") val page: ALSuggestionsPage)
@Serializable data class ALSuggestionsPage(val media: List<ALSuggestionMedia>)

@Serializable
data class ALSuggestionMedia(
    val id: Int,
    val title: ALSuggestionTitle,
    val coverImage: ALSuggestionCoverImage,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val format: String? = null,
    val isAdult: Boolean = false,
)

@Serializable
data class ALSuggestionTitle(
    val userPreferred: String,
    val english: String? = null,
    val romaji: String? = null,
)

@Serializable data class ALSuggestionCoverImage(val large: String)

@Serializable data class ALGenreCollectionResult(val data: ALGenreCollectionData)
@Serializable data class ALGenreCollectionData(
    @SerialName("GenreCollection") val genreCollection: List<String>,
)

// ── Domain model ──────────────────────────────────────────────────────────────

data class AnilistSuggestionItem(
    val id: Int,
    val title: String,
    val coverUrl: String,
    val bannerUrl: String? = null,
    val score: Int?,
    val format: String?,
)

sealed interface RowItemState {
    data object Loading : RowItemState
    data class Success(val items: List<AnilistSuggestionItem>) : RowItemState
    data class Error(val message: String) : RowItemState
}

data class SuggestionRowData(
    val id: String,
    val displayTitle: String,
    val state: RowItemState = RowItemState.Loading,
)

// ── ScreenModel ───────────────────────────────────────────────────────────────

class SuggestionsScreenModel(
    networkHelper: NetworkHelper = Injekt.get(),
) : StateScreenModel<SuggestionsScreenModel.State>(State()) {

    private val json: Json by injectLazy()

    private val anilistHeaders = Headers.Builder()
        .add("Accept", "application/json")
        .add("Content-Type", "application/json")
        .build()

    private val client = networkHelper.nonCloudflareClient.newBuilder()
        .rateLimit(permits = 85, period = 1.minutes)
        .build()

    private val loadedTabs = mutableSetOf<TabKind>()

    init { initializeContent() }

    // ── Public actions ────────────────────────────────────────────────────────

    fun refresh() {
        mutableState.update { it.copy(isRefreshing = true, errorMessage = null) }
        initializeContent()
    }

    fun selectTab(index: Int) {
        mutableState.update { it.copy(selectedTab = index) }
        val kind = if (index == 0) TabKind.Manga else TabKind.Manhwa
        if (kind !in loadedTabs) {
            ioCoroutineScope.launch {
                refreshAll(kind)
                loadedTabs.add(kind)
            }
        }
    }

    fun openFilters() {
        mutableState.update { it.copy(filtersSheetOpen = true) }
    }

    fun closeFilters() {
        mutableState.update { it.copy(filtersSheetOpen = false) }
    }

    fun updateDraftSort(sort: SortOption) = updateDraft { it.copy(sort = sort) }
    fun updateDraftStatus(status: StatusOption?) = updateDraft { it.copy(status = status) }
    fun updateDraftFormat(format: FormatOption?) = updateDraft { it.copy(format = format) }
    fun updateDraftYear(year: Int?) = updateDraft { it.copy(year = year) }

    fun toggleDraftGenre(genre: String) = updateDraft { f ->
        if (genre in f.genres) f.copy(genres = f.genres - genre) else f.copy(genres = f.genres + genre)
    }

    fun resetDraft() {
        mutableState.update { state ->
            val defaults = defaultFiltersForTab(state.selectedTab)
            when (state.selectedTab) {
                0 -> state.copy(draftManga = defaults)
                else -> state.copy(draftManhwa = defaults)
            }
        }
    }

    fun applyDraft() {
        mutableState.update { s ->
            when (s.selectedTab) {
                0 -> s.copy(appliedManga = s.draftManga)
                else -> s.copy(appliedManhwa = s.draftManhwa)
            }
        }
        closeFilters()
        refreshAllForActiveTab()
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private fun initializeContent() {
        ioCoroutineScope.launch {
            val genres = try {
                fetchGenreCollection().filterNot { it.equals("Hentai", ignoreCase = true) }
            } catch (_: Exception) {
                FALLBACK_GENRES
            }
            mutableState.update {
                it.copy(
                    availableGenres = genres,
                    genresLoading = false,
                    isRefreshing = false,
                    errorMessage = null,
                )
            }
            // Load Manga tab on startup; Manhwa is loaded lazily on first visit
            refreshAll(TabKind.Manga)
            loadedTabs.add(TabKind.Manga)
            // If Manhwa was already loaded (e.g. refresh scenario), reload it too
            if (TabKind.Manhwa in loadedTabs) {
                refreshAll(TabKind.Manhwa)
            }
        }
    }

    private fun updateDraft(transform: (Filters) -> Filters) {
        mutableState.update { state ->
            when (state.selectedTab) {
                0 -> state.copy(draftManga = transform(state.draftManga))
                else -> state.copy(draftManhwa = transform(state.draftManhwa))
            }
        }
    }

    private fun refreshAllForActiveTab() {
        ioCoroutineScope.launch {
            val kind = if (mutableState.value.selectedTab == 0) TabKind.Manga else TabKind.Manhwa
            refreshAll(kind)
        }
    }

    private suspend fun refreshAll(kind: TabKind) {
        // Featured shelf (Top Ongoing) and curated shelves
        when (kind) {
            TabKind.Manga -> mutableState.update {
                it.copy(
                    mangaFeatured = ResultsState.Loading,
                    mangaTrending = ResultsState.Loading,
                    mangaPopular = ResultsState.Loading,
                    mangaNew = ResultsState.Loading,
                    mangaTopRated = ResultsState.Loading,
                )
            }
            TabKind.Manhwa -> mutableState.update {
                it.copy(
                    manhwaFeatured = ResultsState.Loading,
                    manhwaTrending = ResultsState.Loading,
                    manhwaPopular = ResultsState.Loading,
                    manhwaNew = ResultsState.Loading,
                    manhwaTopRated = ResultsState.Loading,
                )
            }
        }

        when (kind) {
            TabKind.Manga -> mutableState.update { it.copy(mangaResults = ResultsState.Loading) }
            TabKind.Manhwa -> mutableState.update { it.copy(manhwaResults = ResultsState.Loading) }
        }
        val applied = when (kind) {
            TabKind.Manga -> mutableState.value.appliedManga
            TabKind.Manhwa -> mutableState.value.appliedManhwa
        }
        try {
            coroutineScope {
            val resultsDeferred = async { fetchMedia(kind, applied) }
            val featuredDeferred = async {
                fetchMedia(
                    kind = kind,
                    filters = Filters(
                        sort = SortOption.New,
                        status = StatusOption.Releasing,
                    ),
                    perPage = 12,
                )
            }
            val trendingDeferred = async {
                fetchMedia(kind, Filters(sort = SortOption.Trending), perPage = 20)
            }
            val popularDeferred = async {
                fetchMedia(kind, Filters(sort = SortOption.Popular), perPage = 20)
            }
            val newDeferred = async {
                fetchMedia(kind, Filters(sort = SortOption.New), perPage = 20)
            }
            val topRatedDeferred = async {
                fetchMedia(kind, Filters(sort = SortOption.TopRated), perPage = 20)
            }

            val results = resultsDeferred.await()
            val featured = featuredDeferred.await()
            val trending = trendingDeferred.await()
            val popular = popularDeferred.await()
            val newItems = newDeferred.await()
            val topRated = topRatedDeferred.await()

            mutableState.update {
                when (kind) {
                    TabKind.Manga -> it.copy(
                        mangaResults = ResultsState.Success(results),
                        mangaFeatured = ResultsState.Success(featured),
                        mangaTrending = ResultsState.Success(trending),
                        mangaPopular = ResultsState.Success(popular),
                        mangaNew = ResultsState.Success(newItems),
                        mangaTopRated = ResultsState.Success(topRated),
                    )
                    TabKind.Manhwa -> it.copy(
                        manhwaResults = ResultsState.Success(results),
                        manhwaFeatured = ResultsState.Success(featured),
                        manhwaTrending = ResultsState.Success(trending),
                        manhwaPopular = ResultsState.Success(popular),
                        manhwaNew = ResultsState.Success(newItems),
                        manhwaTopRated = ResultsState.Success(topRated),
                    )
                }
            }
            } // end coroutineScope
        } catch (e: Exception) {
            val msg = e.message ?: "Failed to load"
            mutableState.update {
                when (kind) {
                    TabKind.Manga -> it.copy(
                        mangaResults = ResultsState.Error(msg),
                        mangaFeatured = ResultsState.Error(msg),
                        mangaTrending = ResultsState.Error(msg),
                        mangaPopular = ResultsState.Error(msg),
                        mangaNew = ResultsState.Error(msg),
                        mangaTopRated = ResultsState.Error(msg),
                    )
                    TabKind.Manhwa -> it.copy(
                        manhwaResults = ResultsState.Error(msg),
                        manhwaFeatured = ResultsState.Error(msg),
                        manhwaTrending = ResultsState.Error(msg),
                        manhwaPopular = ResultsState.Error(msg),
                        manhwaNew = ResultsState.Error(msg),
                        manhwaTopRated = ResultsState.Error(msg),
                    )
                }
            }
        }
    }

    private suspend fun fetchMedia(kind: TabKind, filters: Filters, perPage: Int = 60): List<AnilistSuggestionItem> =
        withIOContext {
            val query = $$"""
                query Q(
                  $perPage: Int,
                  $sort: [MediaSort],
                  $country: CountryCode,
                  $status: MediaStatus,
                  $format: MediaFormat,
                  $genreIn: [String],
                  $seasonYear: Int
                ) {
                  Page(page: 1, perPage: $perPage) {
                    media(
                      type: MANGA,
                      sort: $sort,
                      countryOfOrigin: $country,
                      status: $status,
                      format: $format,
                      genre_in: $genreIn,
                      seasonYear: $seasonYear,
                      format_not_in: [NOVEL],
                      isAdult: false
                    ) {
                      id
                      title { userPreferred english romaji }
                      coverImage { large }
                      bannerImage
                      averageScore
                      format
                    }
                  }
                }
            """.trimIndent()

            val country = when (kind) {
                TabKind.Manga -> "JP"
                TabKind.Manhwa -> "KR"
            }

            val variables = buildJsonObject {
                put("perPage", perPage)
                put("country", JsonPrimitive(country))
                putJsonArray("sort") { add(JsonPrimitive(filters.sort.apiValue)) }
                filters.status?.let { put("status", JsonPrimitive(it.apiValue)) }
                filters.format?.let { put("format", JsonPrimitive(it.apiValue)) }
                filters.year?.let { put("seasonYear", it) }
                if (filters.genres.isNotEmpty()) {
                    putJsonArray("genreIn") { filters.genres.forEach { add(JsonPrimitive(it)) } }
                }
            }
            executeQuery(query, variables)
        }

    private suspend fun fetchGenreCollection(): List<String> = withIOContext {
        val payload = buildJsonObject { put("query", "query { GenreCollection }") }
        with(json) {
            client.newCall(
                POST(
                    url = "https://graphql.anilist.co/",
                    headers = anilistHeaders,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            ).awaitSuccess().parseAs<ALGenreCollectionResult>().data.genreCollection
        }
    }

    private suspend fun executeQuery(
        query: String,
        variables: kotlinx.serialization.json.JsonObject,
    ): List<AnilistSuggestionItem> {
        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") {
                variables.entries.forEach { (key, value) -> put(key, value) }
            }
        }
        return with(json) {
            client.newCall(
                POST(
                    url = "https://graphql.anilist.co/",
                    headers = anilistHeaders,
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            )
                .awaitSuccess()
                .parseAs<ALSuggestionsResult>()
                .data.page.media
                .map {
                    AnilistSuggestionItem(
                        id = it.id,
                        title = it.title.english?.takeIf { t -> t.isNotBlank() }
                            ?: it.title.romaji?.takeIf { t -> t.isNotBlank() }
                            ?: it.title.userPreferred,
                        coverUrl = it.coverImage.large,
                        bannerUrl = it.bannerImage,
                        score = it.averageScore,
                        format = it.format,
                    )
                }
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    @Immutable
    data class State(
        val availableGenres: List<String> = emptyList(),
        val genresLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val selectedTab: Int = 0,
        val filtersSheetOpen: Boolean = false,

        val draftManga: Filters = defaultFiltersForTab(0),
        val appliedManga: Filters = defaultFiltersForTab(0),
        val mangaResults: ResultsState = ResultsState.Loading,
        val mangaFeatured: ResultsState = ResultsState.Loading,
        val mangaTrending: ResultsState = ResultsState.Loading,
        val mangaPopular: ResultsState = ResultsState.Loading,
        val mangaNew: ResultsState = ResultsState.Loading,
        val mangaTopRated: ResultsState = ResultsState.Loading,

        val draftManhwa: Filters = defaultFiltersForTab(1),
        val appliedManhwa: Filters = defaultFiltersForTab(1),
        val manhwaResults: ResultsState = ResultsState.Loading,
        val manhwaFeatured: ResultsState = ResultsState.Loading,
        val manhwaTrending: ResultsState = ResultsState.Loading,
        val manhwaPopular: ResultsState = ResultsState.Loading,
        val manhwaNew: ResultsState = ResultsState.Loading,
        val manhwaTopRated: ResultsState = ResultsState.Loading,

        val errorMessage: String? = null,
    )

    companion object {
        val FALLBACK_GENRES = listOf(
            "Action", "Adventure", "Comedy", "Drama", "Fantasy",
            "Horror", "Mystery", "Psychological", "Romance", "Sci-Fi",
            "Slice of Life", "Sports", "Supernatural", "Thriller",
        )
    }
}

private enum class TabKind { Manga, Manhwa }

@Immutable
data class Filters(
    val sort: SortOption = SortOption.Trending,
    val genres: Set<String> = emptySet(),
    val status: StatusOption? = null,
    val format: FormatOption? = null,
    val year: Int? = null,
)

private fun defaultFiltersForTab(tabIndex: Int): Filters {
    // Currently same defaults; hook for future per-tab differences
    return Filters()
}

enum class SortOption(val title: String, val apiValue: String) {
    Trending("Trending", "TRENDING_DESC"),
    Popular("Popular", "POPULARITY_DESC"),
    TopRated("Top Rated", "SCORE_DESC"),
    New("New", "START_DATE_DESC"),
}

enum class StatusOption(val title: String, val apiValue: String) {
    Releasing("Releasing", "RELEASING"),
    Finished("Finished", "FINISHED"),
    Hiatus("Hiatus", "HIATUS"),
    Cancelled("Cancelled", "CANCELLED"),
}

enum class FormatOption(val title: String, val apiValue: String) {
    Manga("Manga", "MANGA"),
    OneShot("One-shot", "ONE_SHOT"),
}

sealed interface ResultsState {
    data object Loading : ResultsState
    data class Success(val items: List<AnilistSuggestionItem>) : ResultsState
    data class Error(val message: String) : ResultsState
}
