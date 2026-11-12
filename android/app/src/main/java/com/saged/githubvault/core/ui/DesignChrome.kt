package com.saged.githubvault.core.ui

import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.saged.githubvault.feature.github.*

@Composable fun DesignHeader(onAccount:()->Unit,modifier:Modifier=Modifier){
 val p=MaterialTheme.colorScheme
 Row(modifier.fillMaxWidth().height(55.dp).background(p.background).padding(start=19.dp,end=19.dp,top=7.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr){Row(horizontalArrangement=Arrangement.spacedBy(9.dp),verticalAlignment=Alignment.CenterVertically){
   Box(Modifier.size(32.dp).clip(RoundedCornerShape(12.dp)).background(p.primary),contentAlignment=Alignment.Center){
    ConceptIcon("stack",modifier=Modifier.size(17.dp),tint=p.onPrimary)
   }
   Text("GitHub Vault",fontFamily=DesignLatinFont,fontSize=12.sp,lineHeight=19.2.sp,fontWeight=FontWeight.Bold)
  }}
  Surface(shape=RoundedCornerShape(16.dp),color=p.surface,border=BorderStroke(1.dp,p.outlineVariant)){MotionIconButton(onClick=onAccount,modifier=Modifier.size(48.dp)){ConceptIcon("person","حسابك",modifier=Modifier.size(23.dp))}}
 }
}
@Composable fun DesignNavigation(selected:String,onSelect:(String)->Unit,activeTransfer:Boolean=false,modifier:Modifier=Modifier){
 val p=MaterialTheme.colorScheme
 Surface(color=p.surface,modifier=modifier){Column{
  HorizontalDivider(color=p.outlineVariant)
  Row(Modifier.fillMaxWidth().height(71.dp).padding(start=7.dp,end=7.dp,top=9.dp,bottom=7.dp)){
   listOf("github" to "مستودعاتك","local" to "عندك","transfers" to "الشغل","settings" to "حسابك").forEach{(id,label)->
    val active=selected==id;val source=remember{MutableInteractionSource()}
    Column(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(13.dp)).pressMotion(source).clickable(interactionSource=source,indication=ripple()){onSelect(id)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(3.dp)){
     Box(Modifier.width(47.dp).height(27.dp).clip(RoundedCornerShape(12.dp)).background(if(active)p.primaryContainer else Color.Transparent),contentAlignment=Alignment.Center){
      BadgedBox(badge={if(id=="transfers"&&activeTransfer)Badge()}){ConceptIcon(when(id){"github"->"folder";"local"->"download";"transfers"->"work";else->"person"},tint=if(active)p.primary else p.onSurfaceVariant,modifier=Modifier.size(20.dp))}
     }
     Text(label,fontSize=9.sp,lineHeight=14.4.sp,color=if(active)p.primary else p.onSurfaceVariant,fontWeight=if(active)FontWeight.Bold else FontWeight.Normal)
    }
   }
  }
 }}
}
@Composable fun PublicAccountScreen(vm:GitHubViewModel,theme:String,onTheme:(String)->Unit,onCloud:()->Unit){
 Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal=18.dp),verticalArrangement=Arrangement.spacedBy(16.dp)){
  BrowserTitle("حسابك","saged7")
  GitHubTokenSettings(vm)
  Text("الشكل",fontWeight=FontWeight.Bold)
  Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){listOf("system" to "زي الجهاز","light" to "فاتح","dark" to "غامق").forEach{(id,label)->MotionFilterChip(selected=theme==id,onClick={onTheme(id)},label={Text(label)})}}
  HorizontalDivider()
  MotionOutlinedButton(onClick=onCloud,modifier=Modifier.fillMaxWidth()){Text("إدارة السحابة")}
 }
}
