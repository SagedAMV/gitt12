package com.saged.githubvault.core.ui
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Dark=darkColorScheme(primary=Color(0xFF77E0C5),onPrimary=Color(0xFF00382B),primaryContainer=Color(0xFF164F43),secondary=Color(0xFFBAC7F6),background=Color(0xFF0D1519),surface=Color(0xFF111D22),surfaceVariant=Color(0xFF203238),onSurface=Color(0xFFE4EEEF),onSurfaceVariant=Color(0xFFAFBEC2),outline=Color(0xFF53666C))
private val Light=lightColorScheme(primary=Color(0xFF006B55),onPrimary=Color.White,primaryContainer=Color(0xFFAEF2DB),secondary=Color(0xFF4C5D92),background=Color(0xFFF3F8F7),surface=Color(0xFFFAFDFC),surfaceVariant=Color(0xFFE2EDE9),onSurface=Color(0xFF142822),onSurfaceVariant=Color(0xFF52675F))
@Composable fun VaultTheme(mode:String="system",content:@Composable ()->Unit) {MaterialTheme(colorScheme=if(mode=="dark" || mode=="system" && isSystemInDarkTheme())Dark else Light,content=content)}
