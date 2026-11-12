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
  Surface(color=background,contentColor=ink,shape=RoundedCornerShape(22.dp),tonalElevation=2.dp,
   modifier=Modifier.fillMaxWidth().semantics{liveRegion=LiveRegionMode.Polite}) {
   Row(Modifier.padding(start=14.dp,end=6.dp,top=12.dp,bottom=12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
    Box(Modifier.size(52.dp),contentAlignment=Alignment.Center) {
     Canvas(Modifier.fillMaxSize()) {
      drawCircle(tint.copy(alpha=.10f))
      val t=burst.value
      drawCircle(tint.copy(alpha=.38f*(1-t)),radius=size.minDimension*(.25f+.25f*t),style=Stroke(2.dp.toPx()))
      drawCircle(tint.copy(alpha=.18f*(1-t)),radius=size.minDimension*(.20f+.22f*t),style=Stroke(1.dp.toPx()))
     }
     Icon(when(held.kind){"success"->Icons.Rounded.VerifiedUser;"info"->Icons.Rounded.History;else->Icons.Rounded.ErrorOutline},contentDescription=null,tint=tint,modifier=Modifier.size(26.dp).scale(.82f+.18f*burst.value))
    }
    Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
     Text(held.title,style=MaterialTheme.typography.titleSmall,fontWeight=FontWeight.Bold)
     Text(held.hint,style=MaterialTheme.typography.bodySmall)
     if(held.kind=="error")Text(held.reference,style=MaterialTheme.typography.labelSmall,color=ink.copy(alpha=.65f))
    }
    MotionTextButton(onClick=onDismiss){Text("تمام",color=ink)}
   }
  }
 }
}
