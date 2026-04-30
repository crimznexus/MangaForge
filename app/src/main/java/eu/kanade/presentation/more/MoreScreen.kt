package eu.kanade.presentation.more

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import eu.kanade.presentation.more.settings.widget.SwitchPreferenceWidget
import eu.kanade.presentation.more.settings.widget.TextPreferenceWidget
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.more.DownloadQueueState
import tachiyomi.core.common.Constants
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

@Composable
fun MoreScreen(
    downloadQueueStateProvider: () -> DownloadQueueState,
    downloadedOnly: Boolean,
    onDownloadedOnlyChange: (Boolean) -> Unit,
    incognitoMode: Boolean,
    onIncognitoModeChange: (Boolean) -> Unit,
    onClickDownloadQueue: () -> Unit,
    onClickCategories: () -> Unit,
    onClickStats: () -> Unit,
    onClickSourcesExtensions: () -> Unit,
    onClickDataAndStorage: () -> Unit,
    onClickSettings: () -> Unit,
    onClickAbout: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val appIconPainter = remember {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        BitmapPainter(drawable.toBitmap().asImageBitmap())
    }

    Scaffold(
        topBar = {
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
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 16.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Image(
                        painter = appIconPainter,
                        contentDescription = null,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Column {
                        Text(
                            text = "MangaForge",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                        Text(
                            text = "Your reading companion",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.75f),
                        )
                    }
                }
            }
        },
    ) { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.padding(contentPadding),
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // ── Quick Settings ───────────────────────────────────────────
            item { MoreSectionLabel(text = "Quick Settings") }
            item {
                MoreCard {
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.label_downloaded_only),
                        subtitle = stringResource(MR.strings.downloaded_only_summary),
                        icon = Icons.Outlined.CloudOff,
                        checked = downloadedOnly,
                        onCheckedChanged = onDownloadedOnlyChange,
                    )
                    MoreDivider()
                    SwitchPreferenceWidget(
                        title = stringResource(MR.strings.pref_incognito_mode),
                        subtitle = stringResource(MR.strings.pref_incognito_mode_summary),
                        icon = ImageVector.vectorResource(R.drawable.ic_glasses_24dp),
                        checked = incognitoMode,
                        onCheckedChanged = onIncognitoModeChange,
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // ── Library ──────────────────────────────────────────────────
            item { MoreSectionLabel(text = "Library") }
            item {
                MoreCard {
                    val downloadQueueState = downloadQueueStateProvider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_download_queue),
                        subtitle = when (downloadQueueState) {
                            DownloadQueueState.Stopped -> null
                            is DownloadQueueState.Paused -> {
                                val pending = downloadQueueState.pending
                                if (pending == 0) {
                                    stringResource(MR.strings.paused)
                                } else {
                                    "${stringResource(MR.strings.paused)} • ${
                                        pluralStringResource(
                                            MR.plurals.download_queue_summary,
                                            count = pending,
                                            pending,
                                        )
                                    }"
                                }
                            }
                            is DownloadQueueState.Downloading -> {
                                val pending = downloadQueueState.pending
                                pluralStringResource(
                                    MR.plurals.download_queue_summary,
                                    count = pending,
                                    pending,
                                )
                            }
                        },
                        icon = Icons.Outlined.GetApp,
                        onPreferenceClick = onClickDownloadQueue,
                    )
                    MoreDivider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.categories),
                        icon = Icons.AutoMirrored.Outlined.Label,
                        onPreferenceClick = onClickCategories,
                    )
                    MoreDivider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_stats),
                        icon = Icons.Outlined.QueryStats,
                        onPreferenceClick = onClickStats,
                    )
                    MoreDivider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_sources),
                        icon = Icons.Outlined.Extension,
                        onPreferenceClick = onClickSourcesExtensions,
                    )
                    MoreDivider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_data_storage),
                        icon = Icons.Outlined.Storage,
                        onPreferenceClick = onClickDataAndStorage,
                    )
                }
            }

            item { Spacer(Modifier.height(4.dp)) }

            // ── App ──────────────────────────────────────────────────────
            item { MoreSectionLabel(text = "App") }
            item {
                MoreCard {
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_settings),
                        icon = Icons.Outlined.Settings,
                        onPreferenceClick = onClickSettings,
                    )
                    MoreDivider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.pref_category_about),
                        icon = Icons.Outlined.Info,
                        onPreferenceClick = onClickAbout,
                    )
                    MoreDivider()
                    TextPreferenceWidget(
                        title = stringResource(MR.strings.label_help),
                        icon = Icons.AutoMirrored.Outlined.HelpOutline,
                        onPreferenceClick = { uriHandler.openUri(Constants.URL_HELP) },
                    )
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun MoreSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 28.dp, top = 8.dp, bottom = 6.dp),
    )
}

@Composable
private fun MoreCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 1.dp,
    ) {
        Column(content = content)
    }
}

@Composable
private fun MoreDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}
