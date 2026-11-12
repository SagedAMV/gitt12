package com.saged.githubvault
import com.saged.githubvault.core.ui.VisualPolicy
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class VisualPolicyTest {
 @Test fun disabledAndReducedMotionNeverShrink(){assertEquals(1f,VisualPolicy.pressedScale(true,false,true));assertEquals(1f,VisualPolicy.pressedScale(true,true,false));assertEquals(1f,VisualPolicy.pressedScale(false,true,true));assertTrue(VisualPolicy.pressedScale(true,true,true) in .95f..1f)}
 @Test fun openingIsShort(){assertTrue(VisualPolicy.OPENING_MS in 800..1500)}
 @Test fun cardsAreOpaque(){assertEquals(1f,VisualPolicy.CARD_ALPHA)}
 @Test fun noRemovedEffectsInAppSource(){
  val root=File("src/main/java/com/saged/githubvault");assertTrue(root.exists())
  val removed=Regex("\\.blur\\s*\\(|RenderEffect|createBlurEffect|GlassCard|AmbientBackdrop")
  root.walkTopDown().filter{it.isFile&&it.extension=="kt"}.forEach{file->assertFalse("Removed effect in ${file.name}",removed.containsMatchIn(file.readText()))}
 }
}
