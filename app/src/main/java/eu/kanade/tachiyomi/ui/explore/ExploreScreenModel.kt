package eu.kanade.tachiyomi.ui.explore

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.produceState
import cafe.adriel.voyager.core.model.StateScreenModel
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.ioCoroutineScope
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.SearchItemResult
import kotlinx.collections.immutable.PersistentMap
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.domain.manga.model.toDomainManga
import tachiyomi.domain.manga.interactor.GetManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher

class ExploreScreenModel(
    sourcePreferences: SourcePreferences = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
    private val networkToLocalManga: NetworkToLocalManga = Injekt.get(),
    private val getManga: GetManga = Injekt.get(),
) : StateScreenModel<ExploreScreenModel.State>(State()) {

    private val coroutineDispatcher = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    private val enabledLanguages = sourcePreferences.enabledLanguages.get()
    private val disabledSources = sourcePreferences.disabledSources.get()
    private val pinnedSources = sourcePreferences.pinnedSources.get()

    init {
        loadPopular()
    }

    private fun getEnabledSources(): List<CatalogueSource> {
        return sourceManager.getCatalogueSources()
            .filter { it.lang in enabledLanguages && "${it.id}" !in disabledSources }
            .sortedWith(
                compareBy(
                    { "${it.id}" !in pinnedSources },
                    { "${it.name.lowercase()} (${it.lang})" },
                ),
            )
    }

    fun loadPopular() {
        val sources = getEnabledSources()
        mutableState.update {
            it.copy(
                items = sources
                    .associateWith { SearchItemResult.Loading }
                    .toPersistentMap(),
            )
        }

        ioCoroutineScope.launch {
            sources.map { source ->
                async {
                    try {
                        val page = withContext(coroutineDispatcher) {
                            source.getPopularManga(1)
                        }
                        val titles = page.mangas
                            .take(15)
                            .map { it.toDomainManga(source.id) }
                            .distinctBy { it.url }
                            .let { networkToLocalManga(it) }
                        if (isActive) updateItem(source, SearchItemResult.Success(titles))
                    } catch (e: Exception) {
                        if (isActive) updateItem(source, SearchItemResult.Error(e))
                    }
                }
            }.awaitAll()
        }
    }

    @Composable
    fun getManga(initialManga: Manga): androidx.compose.runtime.State<Manga> {
        return produceState(initialValue = initialManga) {
            getManga.subscribe(initialManga.url, initialManga.source)
                .filterNotNull()
                .collectLatest { value = it }
        }
    }

    private fun updateItem(source: CatalogueSource, result: SearchItemResult) {
        val newItems = state.value.items.mutate { it[source] = result }
        mutableState.update { it.copy(items = newItems) }
    }

    @Immutable
    data class State(
        val items: PersistentMap<CatalogueSource, SearchItemResult> = persistentMapOf(),
    )
}
