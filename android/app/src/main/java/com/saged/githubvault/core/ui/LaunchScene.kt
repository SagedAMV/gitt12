package com.saged.githubvault.core.ui

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Content starts loading immediately. Opening is skippable, local, and shown once per Activity launch. */
@Composable fun LaunchScene(content:@Composable ()->Unit) {
 var finished by rememberSaveable{mutableStateOf(!ValueAnimator.areAnimatorsEnabled())}
 val reveal=remember{Animatable(if(finished)1f else 0f)}
 LaunchedEffect(Unit){if(!finished){reveal.animateTo(1f,tween(VisualPolicy.OPENING_MS,easing=FastOutSlowInEasing));finished=true}}
 BackHandler(!finished){finished=true}
 val progress=if(finished)1f else reveal.value
 val background=MaterialTheme.colorScheme.background
 Box(Modifier.fillMaxSize().background(background)) {
  Box(Modifier.fillMaxSize().graphicsLayer{alpha=.35f+.65f*progress}){content()}
  if(!finished) {
   val opacity=(1f-((progress-.58f)/.42f).coerceIn(0f,1f))
   // This overlay alone handles taps: no click can fall through into a repo or upload button.
   Box(Modifier.fillMaxSize().background(background.copy(alpha=.92f*(1f-progress)))
    .clickable(interactionSource=remember{MutableInteractionSource()},indication=null,onClick={finished=true}),contentAlignment=Alignment.Center) {
    Column(Modifier.padding(32.dp).graphicsLayer{alpha=opacity;scaleX=.88f+.12f*progress;scaleY=scaleX;translationY=24f*(1f-progress)},horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(18.dp)) {
     MotionCard(modifier=Modifier.size(100.dp),shape=RoundedCornerShape(30.dp)) {
      Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Icon(Icons.Rounded.Shield,null,tint=MaterialTheme.colorScheme.primary,modifier=Modifier.size(48.dp))}
     }
     Text("GitHub Vault",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
     Text("ملفاتك معك",style=MaterialTheme.typography.bodyLarge,color=MaterialTheme.colorScheme.onSurfaceVariant)
     Text("المس وتخطّى",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.primary)
    }
   }
  }
 }
}
