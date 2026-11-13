package com.saged.githubvault
import com.saged.githubvault.core.ui.ScreenTokens
import org.junit.Test
import org.junit.Assert.*
import java.io.File
class HtmlDesignContractTest {
 private val html get()=File("../../docs/design/approved-reference.html").readText().lowercase()
 @Test fun lightPaletteComesFromHtml(){for(c in listOf(ScreenTokens.LIGHT_BG,ScreenTokens.LIGHT_SURFACE,ScreenTokens.LIGHT_INK,ScreenTokens.LIGHT_SUB,ScreenTokens.LIGHT_BORDER,ScreenTokens.LIGHT_TINT,ScreenTokens.BLUE)){val value="#"+c.toString(16).takeLast(6);assertTrue(value,html.contains(value)||value=="#ffffff"&&html.contains("#fff"))}}
 @Test fun darkPaletteComesFromHtml(){for(c in listOf(ScreenTokens.DARK_BG,ScreenTokens.DARK_SURFACE,ScreenTokens.DARK_INK,ScreenTokens.DARK_SUB,ScreenTokens.DARK_BORDER,ScreenTokens.DARK_TINT,ScreenTokens.DARK_BLUE))assertTrue(html.contains("#"+c.toString(16).takeLast(6)))}
 @Test fun actionDimensionsMatch(){assertEquals(52,ScreenTokens.ACTION_HEIGHT);assertEquals(48,ScreenTokens.SEARCH_HEIGHT);assertTrue(html.contains("height:52px"));assertTrue(html.contains("min-height:48px"))}
 @Test fun contentDimensionsMatch(){assertEquals(86,ScreenTokens.REPO_HEIGHT);assertEquals(72,ScreenTokens.FILE_HEIGHT);assertEquals(72,ScreenTokens.NAV_HEIGHT);assertEquals(18,ScreenTokens.PAGE_PADDING);assertTrue(html.contains("min-height:86px"));assertTrue(html.contains("min-height:72px"))}
 @Test fun controlsStayShort(){for(label in listOf("مستودعاتك","دوّر مستودع","دوّر ملف","مستودع جديد","ارفع ملفات","حسابك","النت مش شغال")){assertTrue(html.contains(label));assertTrue(label.split(" ").size<=3)}}
}
