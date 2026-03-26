package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem as M3NavBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRailItem as M3NavRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.explore.ExploreTab
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.suggestions.SuggestionsTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

// ── Brand colours (shared with navigation chrome) ────────────────────────────

private val NavDeep   = Color(0xFF3A0075)
private val NavPurple = Color(0xFF7B2FBE)
private val NavViolet = Color(0xFFCC44FF)
private val NavCoral  = Color(0xFFFF6B6B)

private val NavGradient     = listOf(NavDeep, NavPurple)
private val NavActiveGrad   = listOf(NavPurple, NavViolet)
private val NavExpandedGrad = listOf(NavCoral, NavPurple)

// ── Screen ────────────────────────────────────────────────────────────────────

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    private val TABS = listOf(
        LibraryTab,
        UpdatesTab,
        HistoryTab,
        BrowseTab,
        ExploreTab,
        SuggestionsTab,
        MoreTab,
    )

    private val BOTTOM_NAV_TABS = listOf(SuggestionsTab, LibraryTab, ExploreTab, BrowseTab, MoreTab)
    private val FLOATING_TABS   = listOf(UpdatesTab, HistoryTab)

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(
            tab = SuggestionsTab,
            key = TabNavigatorKey,
        ) { tabNavigator ->
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail {
                                BOTTOM_NAV_TABS.fastForEach {
                                    NavigationRailItem(it)
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isTabletUi()) {
                            val bottomNavVisible by produceState(initialValue = true) {
                                showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                            }
                            AnimatedVisibility(
                                visible = bottomNavVisible,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                NavigationBar {
                                    BOTTOM_NAV_TABS.fastForEach {
                                        NavigationBarItem(it)
                                    }
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(initialScale = 1f, durationMillis = TabFadeDuration) togetherWith
                                    materialFadeThroughOut(durationMillis = TabFadeDuration)
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }

                        FloatingNavCapsule(
                            tabs = FLOATING_TABS,
                            tabNavigator = tabNavigator,
                            navigator = navigator,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(end = 16.dp, bottom = 16.dp),
                        )
                    }
                }
            }

            val goToLibraryTab = { tabNavigator.current = LibraryTab }
            val goToDefaultTab = { tabNavigator.current = SuggestionsTab }
            BackHandler(enabled = tabNavigator.current != SuggestionsTab, onBack = goToDefaultTab)

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        goToLibraryTab()
                        LibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> LibraryTab
                            Tab.Updates -> UpdatesTab
                            Tab.History -> HistoryTab
                            is Tab.Browse -> {
                                if (it.toExtensions) BrowseTab.showExtension()
                                BrowseTab
                            }
                            is Tab.More -> MoreTab
                        }
                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    // ── Floating retractable capsule ──────────────────────────────────────────

    @Composable
    private fun FloatingNavCapsule(
        tabs: List<eu.kanade.presentation.util.Tab>,
        tabNavigator: TabNavigator,
        navigator: Navigator,
        modifier: Modifier = Modifier,
    ) {
        var expanded by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        BackHandler(enabled = expanded) { expanded = false }

        val activeTab = tabs.firstOrNull { tabNavigator.current::class == it::class }

        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Expanded menu — slides up from the toggle button
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(expandFrom = Alignment.Bottom) + fadeIn(),
                exit = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(NavGradient.map { it.copy(alpha = 0.97f) }),
                        )
                        .padding(vertical = 8.dp, horizontal = 8.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        tabs.forEach { tab ->
                            val selected = tabNavigator.current::class == tab::class
                            FloatingTabItemWithLabel(
                                tab = tab,
                                selected = selected,
                                onClick = {
                                    if (!selected) {
                                        tabNavigator.current = tab
                                    } else {
                                        scope.launch { tab.onReselect(navigator) }
                                    }
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }

            // Toggle pill button
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            when {
                                expanded  -> NavExpandedGrad
                                activeTab != null -> NavActiveGrad
                                else      -> NavGradient
                            },
                        ),
                    )
                    .clickable { expanded = !expanded },
                contentAlignment = Alignment.Center,
            ) {
                BadgedBox(
                    badge = { FloatingCapsuleBadge(activeTab) },
                ) {
                    Icon(
                        painter = activeTab?.options?.icon
                            ?: rememberVectorPainter(Icons.Outlined.MoreVert),
                        contentDescription = activeTab?.options?.title ?: "More tabs",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun FloatingCapsuleBadge(activeTab: eu.kanade.presentation.util.Tab?) {
        when {
            activeTab is UpdatesTab || activeTab == null -> {
                val count by produceState(initialValue = 0) {
                    val pref = Injekt.get<LibraryPreferences>()
                    combine(
                        pref.newShowUpdatesCount.changes(),
                        pref.newUpdatesCount.changes(),
                    ) { show, count -> if (show) count else 0 }
                        .collectLatest { value = it }
                }
                if (count > 0 && activeTab == null) {
                    Badge { Text(count.toString()) }
                }
            }
        }
    }

    @Composable
    private fun FloatingTabItemWithLabel(
        tab: eu.kanade.presentation.util.Tab,
        selected: Boolean,
        onClick: () -> Unit,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(if (selected) Color.White.copy(alpha = 0.18f) else Color.Transparent)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = tab.options.title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = Color.White,
            )
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
        }
    }

    // ── Bottom navigation items ───────────────────────────────────────────────

    @Composable
    private fun RowScope.NavigationBarItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        M3NavBarItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab, selected) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = NavPurple,
                selectedTextColor = NavPurple,
                indicatorColor = NavPurple.copy(alpha = 0.12f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }

    @Composable
    fun NavigationRailItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        M3NavRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab, selected) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
            colors = NavigationRailItemDefaults.colors(
                selectedIconColor = NavPurple,
                selectedTextColor = NavPurple,
                indicatorColor = NavPurple.copy(alpha = 0.12f),
                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
    }

    @Composable
    private fun NavigationIconItem(
        tab: eu.kanade.presentation.util.Tab,
        selected: Boolean = false,
    ) {
        BadgedBox(
            badge = {
                when {
                    tab is UpdatesTab -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            combine(
                                pref.newShowUpdatesCount.changes(),
                                pref.newUpdatesCount.changes(),
                            ) { show, count -> if (show) count else 0 }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_chapters_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    BrowseTab::class.isInstance(tab) -> {
                        val count by produceState(initialValue = 0) {
                            Injekt.get<SourcePreferences>().extensionUpdatesCount.changes()
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
                tint = if (selected) NavPurple else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
