package com.saged.githubvault.core.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saged.githubvault.feature.github.SimpleCopy

/** One finite motion burst. Persistent readable guidance; never hides an error on a timer. */
@Composable fun MotionNotice(raw:String?,onDismiss:()->Unit,modifier:Modifier=Modifier) {
 var held by remember{mutableStateOf(SimpleCopy.feedback(raw?:""))}
 LaunchedEffect(raw){if(raw!=null)held=SimpleCopy.feedback(raw)}
 AnimatedVisibility(visible=raw!=null,
  enter=fadeIn(tween(180))+slideInVertically(spring(dampingRatio=.82f)){ -it/3 }+expandVertically(tween(220)),
  exit=fadeOut(tween(140))+shrinkVertically(tween(180)),modifier=modifier) {
  val tint=when(held.kind){"success"->MaterialTheme.colorScheme.primary;"info"->MaterialTheme.colorScheme.tertiary;else->MaterialTheme.colorScheme.error}
  val background=when(held.kind){"success"->MaterialTheme.colorScheme.primaryContainer;"info"->MaterialTheme.colorScheme.tertiaryContainer;else->MaterialTheme.colorScheme.errorContainer}
  val ink=when(held.kind){"success"->MaterialTheme.colorScheme.onPrimaryContainer;"info"->MaterialTheme.colorScheme.onTertiaryContainer;else->MaterialTheme.colorScheme.onErrorContainer}
  val burst=remember{Animatable(0f)}
  LaunchedEffect(held.title){burst.snapTo(0f);burst.animateTo(1f,tween(720,easing=FastOutSlowInEasing))}
  Surface(color=background,contentColor=ink,shape=RoundedCornerShape(17.dp),border=androidx.compose.foundation.BorderStroke(1.dp,tint),tonalElevation=0.dp,
   modifier=Modifier.fillMaxWidth().semantics{liveRegion=LiveRegionMode.Polite}) {
   Row(Modifier.padding(start=12.dp,end=12.dp,top=12.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    Box(Modifier.size(35.dp),contentAlignment=Alignment.Center) {
     Canvas(Modifier.fillMaxSize()) {
      drawCircle(tint.copy(alpha=.10f*(1-burst.value)))
      val t=burst.value
      drawCircle(tint.copy(alpha=.38f*(1-t)),radius=size.minDimension*(.25f+.25f*t),style=Stroke(2.dp.toPx()))
      drawCircle(tint.copy(alpha=.18f*(1-t)),radius=size.minDimension*(.20f+.22f*t),style=Stroke(1.dp.toPx()))
     }
     ConceptIcon(if(held.kind=="error")"alert" else "check",tint=tint,modifier=Modifier.size(23.dp).scale(.82f+.18f*burst.value))
    }
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(0.dp)) {
     Text(held.title,fontSize=12.sp,lineHeight=19.2.sp,fontWeight=FontWeight.Bold)
     Text(held.hint,fontSize=10.sp,lineHeight=16.sp)
     if(held.kind=="error")Text(held.reference,fontFamily=DesignLatinFont,fontSize=10.sp,lineHeight=16.sp,color=ink,modifier=Modifier.align(Alignment.End))
    }
    MotionTextButton(onClick=onDismiss){Text("تمام",fontSize=10.sp,lineHeight=16.sp,color=ink)}
   }
  }
 }
}
