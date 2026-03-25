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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
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
    val averageScore: Int? = null,
    val format: String? = null,
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

    private val client = networkHelper.client.newBuilder()
        .rateLimit(permits = 85, period = 1.minutes)
        .build()

    init { initializeContent() }

    // ── Public actions ────────────────────────────────────────────────────────

    fun refresh() {
        mutableState.update { it.copy(rows = emptyList(), genresLoading = true, selectedGenre = null) }
        initializeContent()
    }

    fun selectTab(index: Int) {
        mutableState.update { it.copy(selectedTab = index, selectedGenre = null) }
    }

    /** null = deselect (show curated rows) */
    fun selectGenre(genre: String?) {
        if (genre == null) {
            mutableState.update { it.copy(selectedGenre = null) }
            return
        }
        val tab = mutableState.value.selectedTab
        val rowId = if (tab == 0) "manga_genre_$genre" else "manhwa_genre_$genre"
        val alreadyExists = mutableState.value.rows.any { it.id == rowId }
        if (!alreadyExists) {
            mutableState.update { state ->
                state.copy(
                    selectedGenre = genre,
                    rows = state.rows + SuggestionRowData(rowId, genre),
                )
            }
            loadSingleRow(rowId)
        } else {
            mutableState.update { it.copy(selectedGenre = genre) }
        }
    }

    // ── Initialisation ────────────────────────────────────────────────────────

    private fun initializeContent() {
        ioCoroutineScope.launch {
            val genres = try {
                fetchGenreCollection().filterNot { it.equals("Hentai", ignoreCase = true) }
            } catch (_: Exception) {
                FALLBACK_GENRES
            }

            val curatedRows = MANGA_CURATED.map { (id, title) -> SuggestionRowData(id, title) } +
                MANHWA_CURATED.map { (id, title) -> SuggestionRowData(id, title) }

            mutableState.update {
                it.copy(rows = curatedRows, availableGenres = genres, genresLoading = false)
            }

            // Load all curated rows in parallel
            ioCoroutineScope.launch {
                (MANGA_CURATED + MANHWA_CURATED).map { (id, _) ->
                    async { loadSingleRow(id) }
                }.awaitAll()
            }
        }
    }

    private fun loadSingleRow(id: String) {
        ioCoroutineScope.launch {
            val result = try {
                RowItemState.Success(fetchRow(id))
            } catch (e: Exception) {
                RowItemState.Error(e.message ?: "Failed to load")
            }
            mutableState.update { state ->
                state.copy(rows = state.rows.map { if (it.id == id) it.copy(state = result) else it })
            }
        }
    }

    // ── Row dispatching ───────────────────────────────────────────────────────

    private suspend fun fetchRow(id: String): List<AnilistSuggestionItem> = when (id) {
        "manga_trending"  -> fetchJapanese("TRENDING_DESC")
        "manga_popular"   -> fetchJapanese("POPULARITY_DESC")
        "manga_new"       -> fetchJapaneseNew()
        "manga_top_rated" -> fetchJapanese("SCORE_DESC")
        "manga_completed" -> fetchJapaneseCompleted()
        "manhwa_trending" -> fetchByCountry("KR", "TRENDING_DESC")
        "manhua_trending" -> fetchByCountry("CN", "TRENDING_DESC")
        "manhwa_popular"  -> fetchByCountry("KR", "POPULARITY_DESC")
        "manhwa_new"      -> fetchManhwaNew()
        "manhwa_completed"-> fetchByCountryCompleted("KR")
        else -> when {
            id.startsWith("manga_genre_")  -> fetchJapaneseByGenre(id.removePrefix("manga_genre_"))
            id.startsWith("manhwa_genre_") -> fetchManhwaByGenre(id.removePrefix("manhwa_genre_"))
            else -> emptyList()
        }
    }

    // ── Query functions ───────────────────────────────────────────────────────

    /** Trending / Popular / Top-rated Japanese manga */
    private suspend fun fetchJapanese(
        sort: String,
        perPage: Int = 20,
    ): List<AnilistSuggestionItem> = withIOContext {
        val query = $$"""
            query Q($perPage: Int) {
              Page(page: 1, perPage: $perPage) {
                media(sort: $$sort, type: MANGA, countryOfOrigin: JP, format_not_in: [NOVEL]) {
                  id title { userPreferred english romaji } coverImage { large } averageScore format
                }
              }
            }
        """.trimIndent()
        executeQuery(query, buildJsonObject { put("perPage", perPage) })
    }

    /** New manga from JP – only series that started recently */
    private suspend fun fetchJapaneseNew(perPage: Int = 20): List<AnilistSuggestionItem> =
        withIOContext {
            val query = $$"""
                query Q($perPage: Int) {
                  Page(page: 1, perPage: $perPage) {
                    media(sort: START_DATE_DESC, type: MANGA, countryOfOrigin: JP,
                          format_not_in: [NOVEL], startDate_greater: 20230101) {
                      id title { userPreferred english romaji } coverImage { large } averageScore format
                    }
                  }
                }
            """.trimIndent()
            executeQuery(query, buildJsonObject { put("perPage", perPage) })
        }

    /** Finished / completed Japanese manga */
    private suspend fun fetchJapaneseCompleted(perPage: Int = 20): List<AnilistSuggestionItem> =
        withIOContext {
            val query = $$"""
                query Q($perPage: Int) {
                  Page(page: 1, perPage: $perPage) {
                    media(sort: POPULARITY_DESC, type: MANGA, countryOfOrigin: JP,
                          format_not_in: [NOVEL], status: FINISHED) {
                      id title { userPreferred english romaji } coverImage { large } averageScore format
                    }
                  }
                }
            """.trimIndent()
            executeQuery(query, buildJsonObject { put("perPage", perPage) })
        }

    /** Trending / Popular Korean/Chinese titles */
    private suspend fun fetchByCountry(
        country: String,
        sort: String,
        perPage: Int = 20,
    ): List<AnilistSuggestionItem> = withIOContext {
        val query = $$"""
            query Q($perPage: Int) {
              Page(page: 1, perPage: $perPage) {
                media(sort: $$sort, type: MANGA, countryOfOrigin: $$country, format_not_in: [NOVEL]) {
                  id title { userPreferred english romaji } coverImage { large } averageScore format
                }
              }
            }
        """.trimIndent()
        executeQuery(query, buildJsonObject { put("perPage", perPage) })
    }

    /** New manhwa/manhua started recently */
    private suspend fun fetchManhwaNew(perPage: Int = 20): List<AnilistSuggestionItem> =
        withIOContext {
            val query = $$"""
                query Q($perPage: Int) {
                  Page(page: 1, perPage: $perPage) {
                    media(sort: START_DATE_DESC, type: MANGA, countryOfOrigin: KR,
                          format_not_in: [NOVEL], startDate_greater: 20230101) {
                      id title { userPreferred english romaji } coverImage { large } averageScore format
                    }
                  }
                }
            """.trimIndent()
            executeQuery(query, buildJsonObject { put("perPage", perPage) })
        }

    /** Completed manhwa */
    private suspend fun fetchByCountryCompleted(
        country: String,
        perPage: Int = 20,
    ): List<AnilistSuggestionItem> = withIOContext {
        val query = $$"""
            query Q($perPage: Int) {
              Page(page: 1, perPage: $perPage) {
                media(sort: POPULARITY_DESC, type: MANGA, countryOfOrigin: $$country,
                      format_not_in: [NOVEL], status: FINISHED) {
                  id title { userPreferred english romaji } coverImage { large } averageScore format
                }
              }
            }
        """.trimIndent()
        executeQuery(query, buildJsonObject { put("perPage", perPage) })
    }

    /** Genre row for Japanese manga */
    private suspend fun fetchJapaneseByGenre(
        genre: String,
        perPage: Int = 20,
    ): List<AnilistSuggestionItem> = withIOContext {
        val query = $$"""
            query Q($perPage: Int, $genre: String) {
              Page(page: 1, perPage: $perPage) {
                media(sort: POPULARITY_DESC, type: MANGA, countryOfOrigin: JP,
                      format_not_in: [NOVEL], genre: $genre) {
                  id title { userPreferred english romaji } coverImage { large } averageScore format
                }
              }
            }
        """.trimIndent()
        executeQuery(query, buildJsonObject { put("perPage", perPage); put("genre", genre) })
    }

    /** Genre row for Korean manhwa */
    private suspend fun fetchManhwaByGenre(
        genre: String,
        perPage: Int = 20,
    ): List<AnilistSuggestionItem> = withIOContext {
        val query = $$"""
            query Q($perPage: Int, $genre: String) {
              Page(page: 1, perPage: $perPage) {
                media(sort: POPULARITY_DESC, type: MANGA, countryOfOrigin: KR,
                      format_not_in: [NOVEL], genre: $genre) {
                  id title { userPreferred english romaji } coverImage { large } averageScore format
                }
              }
            }
        """.trimIndent()
        executeQuery(query, buildJsonObject { put("perPage", perPage); put("genre", genre) })
    }

    private suspend fun fetchGenreCollection(): List<String> = withIOContext {
        val payload = buildJsonObject { put("query", "query { GenreCollection }") }
        with(json) {
            client.newCall(
                POST(
                    url = "https://graphql.anilist.co/",
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
                        score = it.averageScore,
                        format = it.format,
                    )
                }
        }
    }

    // ── State ─────────────────────────────────────────────────────────────────

    @Immutable
    data class State(
        val rows: List<SuggestionRowData> = emptyList(),
        val availableGenres: List<String> = emptyList(),
        val genresLoading: Boolean = true,
        val selectedTab: Int = 0,
        val selectedGenre: String? = null,
    )

    companion object {
        val MANGA_CURATED = listOf(
            "manga_trending"  to "Trending Now",
            "manga_popular"   to "All-Time Popular",
            "manga_new"       to "New Releases",
            "manga_top_rated" to "Top Rated",
            "manga_completed" to "Completed Series",
        )

        val MANHWA_CURATED = listOf(
            "manhwa_trending"  to "Trending Manhwa",
            "manhua_trending"  to "Trending Manhua",
            "manhwa_popular"   to "Popular Manhwa",
            "manhwa_new"       to "New Releases",
            "manhwa_completed" to "Completed Manhwa",
        )

        val FALLBACK_GENRES = listOf(
            "Action", "Adventure", "Comedy", "Drama", "Fantasy",
            "Horror", "Mystery", "Psychological", "Romance", "Sci-Fi",
            "Slice of Life", "Sports", "Supernatural", "Thriller",
        )
    }
}
