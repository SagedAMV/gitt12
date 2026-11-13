package com.saged.githubvault.core.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object ScreenTokens {
 const val LIGHT_BG=0xFFF7F8FC; const val LIGHT_SURFACE=0xFFFFFFFF
 const val LIGHT_INK=0xFF19202C; const val LIGHT_SUB=0xFF5B6477
 const val LIGHT_BORDER=0xFFE1E5EE; const val LIGHT_TINT=0xFFEDF1FE
 const val BLUE=0xFF264BDB
 const val DARK_BG=0xFF131925; const val DARK_SURFACE=0xFF1E2735
 const val DARK_INK=0xFFF4F6FC; const val DARK_SUB=0xFFBBC5D8
 const val DARK_BORDER=0xFF354359; const val DARK_TINT=0xFF293858
 const val DARK_BLUE=0xFFA8BCFF
 const val PAGE_PADDING=18; const val SEARCH_HEIGHT=48; const val ACTION_HEIGHT=52
 const val REPO_HEIGHT=86; const val FILE_HEIGHT=72; const val NAV_HEIGHT=72
}
private val Dark=darkColorScheme(primary=Color(ScreenTokens.DARK_BLUE),onPrimary=Color(0xFF11264D),primaryContainer=Color(ScreenTokens.DARK_TINT),onPrimaryContainer=Color(ScreenTokens.DARK_BLUE),secondary=Color(ScreenTokens.DARK_BLUE),background=Color(ScreenTokens.DARK_BG),surface=Color(ScreenTokens.DARK_SURFACE),surfaceVariant=Color(ScreenTokens.DARK_TINT),onSurface=Color(ScreenTokens.DARK_INK),onBackground=Color(ScreenTokens.DARK_INK),onSurfaceVariant=Color(ScreenTokens.DARK_SUB),outline=Color(ScreenTokens.DARK_BORDER),outlineVariant=Color(ScreenTokens.DARK_BORDER),error=Color(0xFFFFB9C0),errorContainer=Color(0xFF482731),onErrorContainer=Color(0xFFFFB9C0))
private val Light=lightColorScheme(primary=Color(ScreenTokens.BLUE),onPrimary=Color.White,primaryContainer=Color(ScreenTokens.LIGHT_TINT),onPrimaryContainer=Color(ScreenTokens.BLUE),secondary=Color(ScreenTokens.BLUE),background=Color(ScreenTokens.LIGHT_BG),surface=Color(ScreenTokens.LIGHT_SURFACE),surfaceVariant=Color(ScreenTokens.LIGHT_TINT),onSurface=Color(ScreenTokens.LIGHT_INK),onBackground=Color(ScreenTokens.LIGHT_INK),onSurfaceVariant=Color(ScreenTokens.LIGHT_SUB),outline=Color(ScreenTokens.LIGHT_BORDER),outlineVariant=Color(ScreenTokens.LIGHT_BORDER),error=Color(0xFFA62835),errorContainer=Color(0xFFFFF0F1),onErrorContainer=Color(0xFFA62835))

/** Native chrome (status bar, error screen) only; all screens render in the packaged HTML. */
@Composable fun VaultTheme(mode:String="system",content:@Composable ()->Unit) {MaterialTheme(colorScheme=if(mode=="dark" || mode=="system" && isSystemInDarkTheme())Dark else Light,content=content)}
