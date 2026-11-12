package com.saged.githubvault

import android.app.Application
import com.github.takahirom.roborazzi.captureRoboImage
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.*
import com.saged.githubvault.core.ui.*
import com.saged.githubvault.feature.github.*
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.*
import java.io.File

/** Android/Skia native rendering on JVM; not an emulator or a physical-device test. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[35],application=Application::class,qualifiers="w360dp-h720dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignScreenshotTest {
 @get:Rule val compose=createComposeRule()
 @Test fun renderMatchedFixtures()=renderFixtures(360f,640f,"native")
 @Test @Config(qualifiers="w393dp-h873dp-440dpi")
 fun renderRedmiApproximation()=renderFixtures(1080f/2.75f,2202f/2.75f,"redmi-note11pro5g")
 private fun renderFixtures(width:Float,height:Float,directory:String){
  val mode=mutableStateOf("repos-light")
  val repos=listOf("sageer","-").mapIndexed{i,name->buildJsonObject{put("id",i+1);put("name",name);put("default_branch","main")}}
  val files=listOf(Triple("assets",0,"dir"),Triple("README.md",4200,"file"),Triple("app-release.apk",10500000,"file"),Triple("design-notes.pdf",2100000,"file")).map{(name,size,type)->buildJsonObject{put("name",name);put("path",name);put("size",size);put("type",type);put("sha","a".repeat(40))}}
  compose.setContent {
   val choice=mode.value
   CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl){VaultTheme(if(choice.endsWith("dark"))"dark" else "light"){
    CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides androidx.compose.material3.MaterialTheme.colorScheme.onBackground){Column(Modifier.width(width.dp).height(height.dp).background(androidx.compose.material3.MaterialTheme.colorScheme.background).testTag("design")){
     DesignHeader({})
     Box(Modifier.weight(1f)){
      GitHubBrowserContent(GitHubState(repositories=repos,repo=if(choice.startsWith("files"))"sageer"else null,files=files,message=if(choice.startsWith("error"))"النت مش شغال"else null),"",{}, {}, {}, {_,_,_->}, {}, {}, {}, {}, {})
     }
     DesignNavigation("github",{})
    }}
   }}
  }
  val output=File("../../docs/design/$directory").apply{mkdirs()}
  for(name in listOf("repos-light","repos-dark","files-light","files-dark","error-light")){
   compose.runOnIdle{mode.value=name};compose.waitForIdle()
   compose.onNodeWithTag("design").captureRoboImage(File(output,"$name.png"))
   Assert.assertTrue("Screenshot must actually exist: $name",File(output,"$name.png").exists())
  }
 }
}
