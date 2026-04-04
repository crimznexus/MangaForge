package eu.kanade.presentation.theme.colorscheme

import androidx.compose.ui.graphics.Color

/**
 * MangaForge brand themes — three color options derived from the app logo palette.
 *
 * Dark schemes override primaryContainer to a rich deep version of the seed hue
 * so gradient headers read as vivid brand color, not washed-out M3 tonal.
 *
 * Seeds:
 *  - Forge Cyan   #56CCF2  — the signature cyan accent
 *  - Forge Navy   #1A3A99  — the deep blue of the inner circle
 *  - Forge Violet #9B51E0  — the violet gradient accent
 */
internal object MangaForgeCyanColorScheme : BaseColorScheme() {
    private val seed = Color(0xFF56CCF2)
    override val lightScheme = MonetCompatColorScheme.generateColorSchemeFromSeed(seed, dark = false)
    override val darkScheme  = MonetCompatColorScheme.generateColorSchemeFromSeed(seed, dark = true).copy(
        // Richer gradient endpoints in dark mode
        primary = Color(0xFF56CCF2),
        primaryContainer = Color(0xFF0A4F66),
        onPrimary = Color(0xFF001F2A),
        onPrimaryContainer = Color(0xFFB3E9FF),
        background = Color(0xFF060E12),
        surface = Color(0xFF060E12),
    )
}

internal object MangaForgeNavyColorScheme : BaseColorScheme() {
    private val seed = Color(0xFF1A3A99)
    override val lightScheme = MonetCompatColorScheme.generateColorSchemeFromSeed(seed, dark = false)
    override val darkScheme  = MonetCompatColorScheme.generateColorSchemeFromSeed(seed, dark = true).copy(
        primary = Color(0xFF7BA7FF),
        primaryContainer = Color(0xFF0A1A55),
        onPrimary = Color(0xFF00204A),
        onPrimaryContainer = Color(0xFFD8E2FF),
        background = Color(0xFF060810),
        surface = Color(0xFF060810),
    )
}

internal object MangaForgeVioletColorScheme : BaseColorScheme() {
    private val seed = Color(0xFF9B51E0)
    override val lightScheme = MonetCompatColorScheme.generateColorSchemeFromSeed(seed, dark = false)
    override val darkScheme  = MonetCompatColorScheme.generateColorSchemeFromSeed(seed, dark = true).copy(
        primary = Color(0xFFCC88FF),
        primaryContainer = Color(0xFF3A0075),
        onPrimary = Color(0xFF23005C),
        onPrimaryContainer = Color(0xFFEDDAFF),
        background = Color(0xFF090010),
        surface = Color(0xFF090010),
    )
}
