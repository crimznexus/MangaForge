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

@Serializable data class ALDetailResult(val data: ALDetailData)
@Serializable data class ALDetailData(@SerialName("Media") val media: ALDetailMedia)

@Serializable
data class ALDetailMedia(
    val id: Int,
    val title: ALSuggestionTitle,
    val coverImage: ALDetailCoverImage,
    val bannerImage: String? = null,
    val description: String? = null,
    val averageScore: Int? = null,
    val format: String? = null,
    val status: String? = null,
    val genres: List<String> = emptyList(),
    val startDate: ALFuzzyDate? = null,
    val chapters: Int? = null,
    val volumes: Int? = null,
    val popularity: Int? = null,
    val recommendations: ALRecommendationConnection? = null,
)

@Serializable data class ALDetailCoverImage(val large: String, val extraLarge: String? = null)
@Serializable data class ALFuzzyDate(val year: Int? = null)
@Serializable data class ALRecommendationConnection(val nodes: List<ALRecommendationNode> = emptyList())
@Serializable data class ALRecommendationNode(val mediaRecommendation: ALSuggestionMedia? = null)

// ── ScreenModel ───────────────────────────────────────────────────────────────

class MangaDetailScreenModel(
    seed: AnilistSuggestionItem,
    networkHelper: NetworkHelper = Injekt.get(),
) : StateScreenModel<MangaDetailScreenModel.State>(
    State(
        id = seed.id,
        title = seed.title,
        coverUrl = seed.coverUrl,
        score = seed.score,
        format = seed.format,
    ),
) {
    private val json: Json by injectLazy()
    private val client = networkHelper.client.newBuilder()
        .rateLimit(permits = 85, period = 1.minutes)
        .build()

    init { loadDetail(seed.id) }

    private fun loadDetail(id: Int) {
        ioCoroutineScope.launch {
            mutableState.update { it.copy(isLoading = true) }
            try {
                val media = fetchDetail(id)
                val title = media.title.english?.takeIf { it.isNotBlank() }
                    ?: media.title.romaji?.takeIf { it.isNotBlank() }
                    ?: media.title.userPreferred
                val recs = media.recommendations?.nodes
                    ?.mapNotNull { it.mediaRecommendation }
                    ?.filter { !it.isAdult }
                    ?.map { rec ->
                        AnilistSuggestionItem(
                            id = rec.id,
                            title = rec.title.english?.takeIf { it.isNotBlank() }
                                ?: rec.title.romaji?.takeIf { it.isNotBlank() }
                                ?: rec.title.userPreferred,
                            coverUrl = rec.coverImage.large,
                            score = rec.averageScore,
                            format = rec.format,
                        )
                    } ?: emptyList()
                mutableState.update {
                    it.copy(
                        isLoading = false,
                        title = title,
                        coverUrl = media.coverImage.extraLarge ?: media.coverImage.large,
                        bannerUrl = media.bannerImage,
                        description = media.description
                            ?.replace(Regex("<br\\s*/?>"), "\n")
                            ?.replace(Regex("<[^>]+>"), "")
                            ?.trim(),
                        score = media.averageScore,
                        format = media.format,
                        status = media.status
                            ?.lowercase()
                            ?.replace("_", " ")
                            ?.replaceFirstChar { c -> c.uppercase() },
                        genres = media.genres,
                        year = media.startDate?.year,
                        chapters = media.chapters,
                        volumes = media.volumes,
                        recommendations = recs,
                    )
                }
            } catch (e: Exception) {
                mutableState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun fetchDetail(id: Int): ALDetailMedia = withIOContext {
        val query = """
            query Q(${'$'}id: Int) {
              Media(id: ${'$'}id, type: MANGA, isAdult: false) {
                id
                title { userPreferred english romaji }
                coverImage { large extraLarge }
                bannerImage
                description(asHtml: false)
                averageScore
                format
                status
                genres
                startDate { year }
                chapters
                volumes
                popularity
                recommendations(perPage: 8, sort: [RATING_DESC]) {
                  nodes {
                    mediaRecommendation {
                      id
                      title { userPreferred english romaji }
                      coverImage { large }
                      averageScore
                      format
                      isAdult
                    }
                  }
                }
              }
            }
        """.trimIndent()

        val payload = buildJsonObject {
            put("query", query)
            putJsonObject("variables") { put("id", id) }
        }

        with(json) {
            client.newCall(
                POST(
                    url = "https://graphql.anilist.co/",
                    body = payload.toString().toRequestBody(jsonMime),
                ),
            ).awaitSuccess().parseAs<ALDetailResult>().data.media
        }
    }

    @Immutable
    data class State(
        val id: Int,
        val title: String,
        val coverUrl: String,
        val bannerUrl: String? = null,
        val description: String? = null,
        val score: Int? = null,
        val format: String? = null,
        val status: String? = null,
        val genres: List<String> = emptyList(),
        val year: Int? = null,
        val chapters: Int? = null,
        val volumes: Int? = null,
        val recommendations: List<AnilistSuggestionItem> = emptyList(),
        val isLoading: Boolean = false,
        val error: String? = null,
    )
}
