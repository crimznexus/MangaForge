package eu.kanade.presentation.manga.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.AttachMoney
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Done
import androidx.compose.material.icons.outlined.DoneAll
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.BitmapImage
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult as CoilSuccessResult
import coil3.request.crossfade
import com.mikepenz.markdown.model.markdownAnnotator
import com.mikepenz.markdown.model.markdownAnnotatorConfig
import com.mikepenz.markdown.utils.getUnescapedTextInNode
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.DropdownMenu
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.util.system.copyToClipboard
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.findChildOfType
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.components.material.TextButton
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.clickableNoIndication
import tachiyomi.presentation.core.util.secondaryItemAlpha
import androidx.palette.graphics.Palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt


@Composable
fun MangaInfoBox(
    isTabletUi: Boolean,
    appBarPadding: Dp,
    manga: Manga,
    sourceName: String,
    isStubSource: Boolean,
    onCoverClick: () -> Unit,
    doSearch: (query: String, global: Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val heroHeight = if (isTabletUi) 320.dp else 280.dp
    val context = LocalContext.current
    var accentColor by remember { mutableStateOf(Color(0xFF1A3A99)) }
    LaunchedEffect(manga.thumbnailUrl) {
        try {
            val req = ImageRequest.Builder(context)
                .data(manga)
                .size(64, 64)
                .build()
            val result = withContext(Dispatchers.IO) { context.imageLoader.execute(req) }
            if (result is CoilSuccessResult) {
                val raw = (result.image as? BitmapImage)?.bitmap
                if (raw != null) {
                    val bitmap = if (raw.config == android.graphics.Bitmap.Config.HARDWARE) {
                        raw.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
                    } else raw
                    val palette = withContext(Dispatchers.IO) { Palette.from(bitmap).generate() }
                    val dominant = palette.getDominantColor(0xFF1A3A99.toInt())
                    accentColor = Color(dominant)
                }
            }
        } catch (_: Exception) { }
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heroHeight + appBarPadding)
            .clickable(onClick = onCoverClick),
    ) {
        // Full-bleed cover image
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(manga)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        // Gradient overlay using cover's extracted palette accent
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.15f),
                        0.3f to accentColor.copy(alpha = 0.50f),
                        0.65f to accentColor.copy(alpha = 0.85f),
                        1f to MaterialTheme.colorScheme.background,
                    ),
                ),
        )
        // Metadata pinned to bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MangaStatusBadge(status = manga.status)
            Text(
                text = manga.title.ifBlank { stringResource(MR.strings.unknown_title) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://anilist.co/search/manga?search=${Uri.encode(manga.title)}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
            val author = manga.author
            if (!author.isNullOrBlank()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PersonOutline,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = Color.White.copy(alpha = 0.75f),
                    )
                    Text(
                        text = author,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.75f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = { doSearch(author, true) }),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (isStubSource) {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
                Text(
                    text = sourceName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = { doSearch(sourceName, false) }),
                )
            }
        }
    }
}

@Composable
private fun MangaStatusBadge(status: Long) {
    val (icon, label) = when (status) {
        SManga.ONGOING.toLong() -> Icons.Outlined.Schedule to stringResource(MR.strings.ongoing)
        SManga.COMPLETED.toLong() -> Icons.Outlined.DoneAll to stringResource(MR.strings.completed)
        SManga.LICENSED.toLong() -> Icons.Outlined.AttachMoney to stringResource(MR.strings.licensed)
        SManga.PUBLISHING_FINISHED.toLong() -> Icons.Outlined.Done to stringResource(MR.strings.publishing_finished)
        SManga.CANCELLED.toLong() -> Icons.Outlined.Close to stringResource(MR.strings.cancelled)
        SManga.ON_HIATUS.toLong() -> Icons.Outlined.Pause to stringResource(MR.strings.on_hiatus)
        else -> Icons.Outlined.Block to stringResource(MR.strings.unknown)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.80f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(11.dp), tint = Color.White)
        Text(text = label, fontSize = 10.sp, color = Color.White)
    }
}

@Composable
fun MangaActionRow(
    favorite: Boolean,
    trackingCount: Int,
    nextUpdate: Instant?,
    isUserIntervalMode: Boolean,
    onAddToLibraryClicked: () -> Unit,
    onWebViewClicked: (() -> Unit)?,
    onWebViewLongClicked: (() -> Unit)?,
    onTrackingClicked: () -> Unit,
    onEditIntervalClicked: (() -> Unit)?,
    onEditCategory: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val nextUpdateDays = remember(nextUpdate) {
        if (nextUpdate != null) {
            val now = Instant.now()
            now.until(nextUpdate, ChronoUnit.DAYS).toInt().coerceAtLeast(0)
        } else {
            null
        }
    }

    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Primary: full-width gradient pill
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)))
                .combinedClickable(
                    onClick = onAddToLibraryClicked,
                    onLongClick = { onEditCategory?.invoke() },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = if (favorite) stringResource(MR.strings.in_library) else stringResource(MR.strings.add_to_library),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Secondary chips row
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MangaSecondaryChip(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.HourglassEmpty,
                label = when (nextUpdateDays) {
                    null -> stringResource(MR.strings.not_applicable)
                    0 -> stringResource(MR.strings.manga_interval_expected_update_soon)
                    else -> pluralStringResource(MR.plurals.day, count = nextUpdateDays, nextUpdateDays)
                },
                active = isUserIntervalMode,
                onClick = { onEditIntervalClicked?.invoke() },
            )
            MangaSecondaryChip(
                modifier = Modifier.weight(1f),
                icon = if (trackingCount == 0) Icons.Outlined.Sync else Icons.Outlined.Done,
                label = if (trackingCount == 0) {
                    stringResource(MR.strings.manga_tracking_tab)
                } else {
                    pluralStringResource(MR.plurals.num_trackers, count = trackingCount, trackingCount)
                },
                active = trackingCount > 0,
                onClick = onTrackingClicked,
            )
            if (onWebViewClicked != null) {
                MangaSecondaryChip(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Public,
                    label = stringResource(MR.strings.action_web_view),
                    active = false,
                    onClick = onWebViewClicked,
                    onLongClick = onWebViewLongClicked,
                )
            }
        }
    }
}

@Composable
private fun MangaSecondaryChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
            .border(
                width = 1.dp,
                color = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f),
                shape = RoundedCornerShape(12.dp),
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.55f),
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.55f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
fun ExpandableMangaDescription(
    defaultExpandState: Boolean,
    description: String?,
    tagsProvider: () -> List<String>?,
    notes: String,
    onTagSearch: (String) -> Unit,
    onCopyTagToClipboard: (tag: String) -> Unit,
    onEditNotes: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        val (expanded, onExpanded) = rememberSaveable {
            mutableStateOf(defaultExpandState)
        }
        val desc =
            description.takeIf { !it.isNullOrBlank() } ?: stringResource(MR.strings.description_placeholder)

        MangaSummary(
            description = desc,
            expanded = expanded,
            notes = notes,
            onEditNotesClicked = onEditNotes,
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 16.dp)
                .clickableNoIndication { onExpanded(!expanded) },
        )
        val tags = tagsProvider()
        if (!tags.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .padding(vertical = 12.dp)
                    .animateContentSize(animationSpec = spring())
                    .fillMaxWidth(),
            ) {
                if (expanded) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        tags.forEach { tag ->
                            key(tag) {
                                var showMenu by remember { mutableStateOf(false) }
                                Box {
                                    TagsChip(
                                        modifier = DefaultTagChipModifier,
                                        text = tag,
                                        onClick = {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://anilist.co/search/manga?search=${Uri.encode(tag)}"))
                                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                            )
                                        },
                                        onLongClick = { showMenu = true },
                                    )
                                    DropdownMenu(
                                        expanded = showMenu,
                                        onDismissRequest = { showMenu = false },
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(MR.strings.action_search)) },
                                            onClick = { onTagSearch(tag); showMenu = false },
                                        )
                                        DropdownMenuItem(
                                            text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
                                            onClick = { onCopyTagToClipboard(tag); showMenu = false },
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = MaterialTheme.padding.medium),
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall),
                    ) {
                        items(items = tags) { tag ->
                            var showMenu by remember { mutableStateOf(false) }
                            Box {
                                TagsChip(
                                    modifier = DefaultTagChipModifier,
                                    text = tag,
                                    onClick = {
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse("https://anilist.co/search/manga?search=${Uri.encode(tag)}"))
                                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                        )
                                    },
                                    onLongClick = { showMenu = true },
                                )
                                DropdownMenu(
                                    expanded = showMenu,
                                    onDismissRequest = { showMenu = false },
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(MR.strings.action_search)) },
                                        onClick = { onTagSearch(tag); showMenu = false },
                                    )
                                    DropdownMenuItem(
                                        text = { Text(text = stringResource(MR.strings.action_copy_to_clipboard)) },
                                        onClick = { onCopyTagToClipboard(tag); showMenu = false },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun descriptionAnnotator(loadImages: Boolean, linkStyle: SpanStyle) = remember(loadImages, linkStyle) {
    markdownAnnotator(
        annotate = { content, child ->
            if (!loadImages && child.type == MarkdownElementTypes.IMAGE) {
                val inlineLink = child.findChildOfType(MarkdownElementTypes.INLINE_LINK)

                val url = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_DESTINATION)
                    ?.getUnescapedTextInNode(content)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.AUTOLINK)
                        ?.findChildOfType(MarkdownTokenTypes.AUTOLINK)
                        ?.getUnescapedTextInNode(content)
                    ?: return@markdownAnnotator false

                val textNode = inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TITLE)
                    ?: inlineLink?.findChildOfType(MarkdownElementTypes.LINK_TEXT)
                val altText = textNode?.findChildOfType(MarkdownTokenTypes.TEXT)
                    ?.getUnescapedTextInNode(content).orEmpty()

                withLink(LinkAnnotation.Url(url = url)) {
                    pushStyle(linkStyle)
                    appendInlineContent(MARKDOWN_INLINE_IMAGE_TAG)
                    append(altText)
                    pop()
                }

                return@markdownAnnotator true
            }

            if (child.type in DISALLOWED_MARKDOWN_TYPES) {
                append(content.substring(child.startOffset, child.endOffset))
                return@markdownAnnotator true
            }

            false
        },
        config = markdownAnnotatorConfig(
            eolAsNewLine = true,
        ),
    )
}

@Composable
private fun MangaSummary(
    description: String,
    notes: String,
    expanded: Boolean,
    onEditNotesClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = remember { Injekt.get<UiPreferences>() }
    val loadImages = remember { preferences.imagesInDescription.get() }
    val animProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        label = "summary",
    )
    var infoHeight by remember { mutableIntStateOf(0) }
    Layout(
        modifier = modifier.clipToBounds(),
        contents = listOf(
            {
                Text(
                    // Shows at least 3 lines if no notes
                    // when there are notes show 6
                    text = if (notes.isBlank()) "\n\n" else "\n\n\n\n\n",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            {
                Column(
                    modifier = Modifier.onSizeChanged { size ->
                        infoHeight = size.height
                    },
                ) {
                    MangaNotesSection(
                        content = notes,
                        expanded = expanded,
                        onEditNotes = onEditNotesClicked,
                    )
                    SelectionContainer {
                        MarkdownRender(
                            content = description,
                            modifier = Modifier.secondaryItemAlpha(),
                            annotator = descriptionAnnotator(
                                loadImages = loadImages,
                                linkStyle = getMarkdownLinkStyle().toSpanStyle(),
                            ),
                            loadImages = loadImages,
                        )
                    }
                }
            },
            {
                val colors = listOf(Color.Transparent, MaterialTheme.colorScheme.background)
                Box(
                    modifier = Modifier.background(Brush.verticalGradient(colors = colors)),
                    contentAlignment = Alignment.Center,
                ) {
                    val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_caret_down)
                    Icon(
                        painter = rememberAnimatedVectorPainter(image, !expanded),
                        contentDescription = stringResource(
                            if (expanded) MR.strings.manga_info_collapse else MR.strings.manga_info_expand,
                        ),
                        tint = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.background(Brush.radialGradient(colors = colors.asReversed())),
                    )
                }
            },
        ),
    ) { (shrunk, actual, scrim), constraints ->
        val shrunkHeight = shrunk.single()
            .measure(constraints)
            .height
        val heightDelta = infoHeight - shrunkHeight
        val scrimHeight = 24.dp.roundToPx()

        val actualPlaceable = actual.single()
            .measure(constraints)
        val scrimPlaceable = scrim.single()
            .measure(Constraints.fixed(width = constraints.maxWidth, height = scrimHeight))

        val currentHeight = shrunkHeight + ((heightDelta + scrimHeight) * animProgress).roundToInt()
        layout(constraints.maxWidth, currentHeight) {
            actualPlaceable.place(0, 0)

            val scrimY = currentHeight - scrimHeight
            scrimPlaceable.place(0, scrimY)
        }
    }
}

private val DefaultTagChipModifier = Modifier.padding(vertical = 4.dp)

@Composable
private fun TagsChip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
) {
    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f))
                .border(1.dp, MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.40f), RoundedCornerShape(8.dp))
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

