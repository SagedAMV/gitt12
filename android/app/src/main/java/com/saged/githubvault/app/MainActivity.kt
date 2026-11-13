package com.saged.githubvault.app
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.saged.githubvault.core.ui.VaultTheme
import com.saged.githubvault.core.ui.WebVault
class MainActivity:ComponentActivity() {
 override fun onCreate(savedInstanceState:Bundle?) {
  super.onCreate(savedInstanceState)
  androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window,false)
  val prefs=getSharedPreferences("preferences",MODE_PRIVATE)
  setContent {
   var theme by remember{mutableStateOf(prefs.getString("theme","system")?:"system")}
   CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    VaultTheme(theme) { WebVault(theme=theme,onTheme={theme=it;prefs.edit().putString("theme",it).apply()}) }
   }
  }
 }
}
