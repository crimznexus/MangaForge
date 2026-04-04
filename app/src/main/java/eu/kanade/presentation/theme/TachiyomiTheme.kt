package eu.kanade.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.AppTheme
import eu.kanade.presentation.theme.colorscheme.BaseColorScheme
import eu.kanade.presentation.theme.colorscheme.MangaForgeCyanColorScheme
import eu.kanade.presentation.theme.colorscheme.MangaForgeNavyColorScheme
import eu.kanade.presentation.theme.colorscheme.MangaForgeVioletColorScheme
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

@Composable
fun TachiyomiTheme(
    appTheme: AppTheme? = null,
    amoled: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val uiPreferences = Injekt.get<UiPreferences>()
    BaseTachiyomiTheme(
        appTheme = appTheme ?: uiPreferences.appTheme.get(),
        isAmoled = amoled ?: uiPreferences.themeDarkAmoled.get(),
        content = content,
    )
}

@Composable
fun TachiyomiPreviewTheme(
    appTheme: AppTheme = AppTheme.MANGAFORGE_NAVY,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) = BaseTachiyomiTheme(appTheme, isAmoled, content)

@Composable
private fun BaseTachiyomiTheme(
    appTheme: AppTheme,
    isAmoled: Boolean,
    content: @Composable () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    MaterialExpressiveTheme(
        colorScheme = remember(appTheme, isDark, isAmoled) {
            getThemeColorScheme(
                appTheme = appTheme,
                isDark = isDark,
                isAmoled = isAmoled,
            )
        },
        content = content,
    )
}

private fun getThemeColorScheme(
    appTheme: AppTheme,
    isDark: Boolean,
    isAmoled: Boolean,
): ColorScheme {
    val colorScheme = colorSchemes.getOrDefault(appTheme, MangaForgeNavyColorScheme)
    return colorScheme.getColorScheme(
        isDark = isDark,
        isAmoled = isAmoled,
        overrideDarkSurfaceContainers = true,
    )
}

private val colorSchemes: Map<AppTheme, BaseColorScheme> = mapOf(
    AppTheme.MANGAFORGE_CYAN to MangaForgeCyanColorScheme,
    AppTheme.MANGAFORGE_NAVY to MangaForgeNavyColorScheme,
    AppTheme.MANGAFORGE_VIOLET to MangaForgeVioletColorScheme,
)
