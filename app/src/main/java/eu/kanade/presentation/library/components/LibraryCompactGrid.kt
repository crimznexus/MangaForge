package eu.kanade.presentation.library.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.tachiyomi.ui.library.LibraryItem
import tachiyomi.domain.library.model.LibraryManga
import tachiyomi.domain.manga.model.MangaCover

@Composable
internal fun LibraryCompactGrid(
    items: List<LibraryItem>,
    showTitle: Boolean,
    columns: Int,
    contentPadding: PaddingValues,
    selection: Set<Long>,
    onClick: (LibraryManga) -> Unit,
    onLongClick: (LibraryManga) -> Unit,
    onClickContinueReading: ((LibraryManga) -> Unit)?,
    searchQuery: String?,
    onGlobalSearchClicked: () -> Unit,
) {
    LazyLibraryGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = contentPadding,
    ) {
        globalSearchItem(searchQuery, onGlobalSearchClicked)

        // Pick featured item: most-recently-read if any started, else first
        val featuredItem = items.filter { it.libraryManga.hasStarted }
            .maxByOrNull { it.libraryManga.lastRead }
            ?: items.first()

        item(
            span = { GridItemSpan(maxLineSpan) },
            contentType = "library_featured_item",
        ) {
            val manga = featuredItem.libraryManga.manga
            MangaFeaturedGridItem(
                isSelected = manga.id in selection,
                title = manga.title,
                readCount = featuredItem.libraryManga.readCount,
                totalChapters = featuredItem.libraryManga.totalChapters,
                coverData = MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                ),
                coverBadgeStart = {
                    DownloadsBadge(count = featuredItem.downloadCount)
                    UnreadBadge(count = featuredItem.unreadCount)
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        isLocal = featuredItem.isLocal,
                        sourceLanguage = featuredItem.sourceLanguage,
                    )
                },
                onLongClick = { onLongClick(featuredItem.libraryManga) },
                onClick = { onClick(featuredItem.libraryManga) },
                onClickContinueReading = if (onClickContinueReading != null && featuredItem.unreadCount > 0) {
                    { onClickContinueReading(featuredItem.libraryManga) }
                } else {
                    null
                },
            )
        }

        items(
            items = items.filter { it.id != featuredItem.id },
            contentType = { "library_compact_grid_item" },
        ) { libraryItem ->
            val manga = libraryItem.libraryManga.manga
            MangaCompactGridItem(
                isSelected = manga.id in selection,
                title = manga.title.takeIf { showTitle },
                coverData = MangaCover(
                    mangaId = manga.id,
                    sourceId = manga.source,
                    isMangaFavorite = manga.favorite,
                    url = manga.thumbnailUrl,
                    lastModified = manga.coverLastModified,
                ),
                coverBadgeStart = {
                    DownloadsBadge(count = libraryItem.downloadCount)
                    UnreadBadge(count = libraryItem.unreadCount)
                },
                coverBadgeEnd = {
                    LanguageBadge(
                        isLocal = libraryItem.isLocal,
                        sourceLanguage = libraryItem.sourceLanguage,
                    )
                },
                onLongClick = { onLongClick(libraryItem.libraryManga) },
                onClick = { onClick(libraryItem.libraryManga) },
                onClickContinueReading = if (onClickContinueReading != null && libraryItem.unreadCount > 0) {
                    { onClickContinueReading(libraryItem.libraryManga) }
                } else {
                    null
                },
            )
        }
    }
}
