package eu.kanade.tachiyomi.ui.updates

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.updates.UpdateScreen
import eu.kanade.presentation.updates.UpdatesDeleteConfirmationDialog
import eu.kanade.presentation.updates.UpdatesFilterDialog
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.main.MainActivity
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.ui.updates.UpdatesScreenModel.Event
import kotlinx.coroutines.flow.collectLatest
import mihon.feature.upcoming.UpcomingScreen
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object UpdatesTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_updates_enter)
            return TabOptions(
                index = 2u,
                title = stringResource(MR.strings.label_recent_updates),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    override suspend fun onReselect(navigator: Navigator) {
        navigator.push(DownloadQueueScreen)
    }

    @Composable
    override fun Content() {
        var selectedSubTab by rememberSaveable { mutableIntStateOf(0) }

        Column {
            // Gradient header with toggle chips
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primaryContainer,
                                MaterialTheme.colorScheme.primary,
                            ),
                        ),
                    ),
            ) {
                Column(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 4.dp),
                ) {
                    Text(
                        text = stringResource(MR.strings.label_recent_updates),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        listOf(
                            stringResource(MR.strings.label_recent_updates),
                            stringResource(MR.strings.label_recent_manga),
                        ).forEachIndexed { index, label ->
                            FilterChip(
                                selected = selectedSubTab == index,
                                onClick = { selectedSubTab = index },
                                label = { Text(label, style = MaterialTheme.typography.labelMedium) },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.Transparent,
                                    selectedContainerColor = Color.White.copy(alpha = 0.22f),
                                    labelColor = Color.White.copy(alpha = 0.65f),
                                    selectedLabelColor = Color.White,
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selectedSubTab == index,
                                    selectedBorderColor = Color.White.copy(alpha = 0.55f),
                                    borderColor = Color.White.copy(alpha = 0.28f),
                                ),
                            )
                        }
                    }
                }
            }

            when (selectedSubTab) {
                0 -> {
                    val context = LocalContext.current
                    val navigator = LocalNavigator.currentOrThrow
                    val screenModel = rememberScreenModel { UpdatesScreenModel() }
                    val settingsScreenModel = rememberScreenModel { UpdatesSettingsScreenModel() }
                    val state by screenModel.state.collectAsState()

                    UpdateScreen(
                        state = state,
                        snackbarHostState = screenModel.snackbarHostState,
                        lastUpdated = screenModel.lastUpdated,
                        onClickCover = { item -> navigator.push(MangaScreen(item.update.mangaId)) },
                        onSelectAll = screenModel::toggleAllSelection,
                        onInvertSelection = screenModel::invertSelection,
                        onUpdateLibrary = screenModel::updateLibrary,
                        onDownloadChapter = screenModel::downloadChapters,
                        onMultiBookmarkClicked = screenModel::bookmarkUpdates,
                        onMultiMarkAsReadClicked = screenModel::markUpdatesRead,
                        onMultiDeleteClicked = screenModel::showConfirmDeleteChapters,
                        onUpdateSelected = screenModel::toggleSelection,
                        onOpenChapter = {
                            val intent = ReaderActivity.newIntent(context, it.update.mangaId, it.update.chapterId)
                            context.startActivity(intent)
                        },
                        onCalendarClicked = { navigator.push(UpcomingScreen()) },
                        onFilterClicked = screenModel::showFilterDialog,
                        hasActiveFilters = state.hasActiveFilters,
                    )

                    val onDismissDialog = { screenModel.setDialog(null) }
                    when (val dialog = state.dialog) {
                        is UpdatesScreenModel.Dialog.DeleteConfirmation -> {
                            UpdatesDeleteConfirmationDialog(
                                onDismissRequest = onDismissDialog,
                                onConfirm = { screenModel.deleteChapters(dialog.toDelete) },
                            )
                        }
                        is UpdatesScreenModel.Dialog.FilterSheet -> {
                            UpdatesFilterDialog(
                                onDismissRequest = onDismissDialog,
                                screenModel = settingsScreenModel,
                            )
                        }
                        null -> {}
                    }

                    LaunchedEffect(Unit) {
                        screenModel.events.collectLatest { event ->
                            when (event) {
                                Event.InternalError -> screenModel.snackbarHostState.showSnackbar(
                                    context.stringResource(MR.strings.internal_error),
                                )
                                is Event.LibraryUpdateTriggered -> {
                                    val msg = if (event.started) {
                                        MR.strings.updating_library
                                    } else {
                                        MR.strings.update_already_running
                                    }
                                    screenModel.snackbarHostState.showSnackbar(context.stringResource(msg))
                                }
                            }
                        }
                    }

                    LaunchedEffect(state.selectionMode) {
                        HomeScreen.showBottomNav(!state.selectionMode)
                    }

                    LaunchedEffect(state.isLoading) {
                        if (!state.isLoading) {
                            (context as? MainActivity)?.ready = true
                        }
                    }
                    DisposableEffect(Unit) {
                        screenModel.resetNewUpdatesCount()
                        onDispose {
                            screenModel.resetNewUpdatesCount()
                        }
                    }
                }
                else -> HistoryTab.Content()
            }
        }
    }
}
