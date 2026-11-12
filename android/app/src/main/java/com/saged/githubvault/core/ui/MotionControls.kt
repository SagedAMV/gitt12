@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class,androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.saged.githubvault.core.ui

import android.animation.ValueAnimator
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.dp

/** Pure values also covered by JVM regression tests. */
object VisualPolicy {
 fun pressedScale(pressed:Boolean,enabled:Boolean,motion:Boolean)=if(pressed&&enabled&&motion).965f else 1f
 const val CARD_ALPHA=1f
 const val OPENING_MS=1150
}

@Composable internal fun Modifier.pressMotion(source:MutableInteractionSource,enabled:Boolean=true):Modifier {
 val pressed by source.collectIsPressedAsState()
 val target=VisualPolicy.pressedScale(pressed,enabled,ValueAnimator.areAnimatorsEnabled())
 val scale by animateFloatAsState(target,spring(dampingRatio=.68f,stiffness=650f),label="press-rebound")
 return graphicsLayer{scaleX=scale;scaleY=scale}
}

@Composable fun MotionCard(
 modifier:Modifier=Modifier,
 shape:Shape=RoundedCornerShape(22.dp),
 colors:CardColors?=null,
 onClick:(()->Unit)?=null,
 enabled:Boolean=true,
 onLongClick:(()->Unit)?=null,
 border:BorderStroke?=null,
 content:@Composable ColumnScope.()->Unit
) {
 val palette=MaterialTheme.colorScheme
 val source=remember{MutableInteractionSource()}
 val base=colors?.containerColor?:palette.surface
 val interactive=if(onClick!=null||onLongClick!=null)Modifier.pressMotion(source,enabled).combinedClickable(interactionSource=source,indication=ripple(),enabled=enabled,onClick=onClick?:{},onLongClick=onLongClick)else Modifier
 Surface(modifier=modifier.clip(shape).then(interactive),shape=shape,
  color=base.copy(alpha=VisualPolicy.CARD_ALPHA),contentColor=palette.onSurface,
  border=border?:BorderStroke(1.dp,palette.outlineVariant),shadowElevation=0.dp) {
  Column(content=content)
 }
}

@Composable fun MotionButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=ButtonDefaults.shape,content:@Composable RowScope.()->Unit) {
 val source=remember{MutableInteractionSource()}
 Button(onClick,modifier.pressMotion(source,enabled),enabled=enabled,shape=shape,interactionSource=source,content=content)
}
@Composable fun MotionFilledTonalButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=ButtonDefaults.shape,content:@Composable RowScope.()->Unit) {
 val source=remember{MutableInteractionSource()}
 FilledTonalButton(onClick,modifier.pressMotion(source,enabled),enabled=enabled,shape=shape,interactionSource=source,content=content)
}
@Composable fun MotionOutlinedButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,shape:Shape=ButtonDefaults.shape,content:@Composable RowScope.()->Unit) {
 val source=remember{MutableInteractionSource()}
 OutlinedButton(onClick,modifier.pressMotion(source,enabled),enabled=enabled,shape=shape,interactionSource=source,content=content)
}
@Composable fun MotionTextButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,contentPadding:PaddingValues=ButtonDefaults.TextButtonContentPadding,content:@Composable RowScope.()->Unit) {
 val source=remember{MutableInteractionSource()}
 TextButton(onClick,modifier.pressMotion(source,enabled),enabled=enabled,contentPadding=contentPadding,interactionSource=source,content=content)
}
@Composable fun MotionIconButton(onClick:()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,content:@Composable ()->Unit) {
 val source=remember{MutableInteractionSource()}
 IconButton(onClick,modifier.pressMotion(source,enabled),enabled=enabled,interactionSource=source,content=content)
}
@Composable fun MotionFilterChip(selected:Boolean,onClick:()->Unit,label:@Composable ()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true) {
 val source=remember{MutableInteractionSource()}
 val p=MaterialTheme.colorScheme
 Surface(onClick=onClick,modifier=modifier.pressMotion(source,enabled).heightIn(min=48.dp),enabled=enabled,shape=RoundedCornerShape(11.dp),color=if(selected)p.primaryContainer else p.surface,contentColor=if(selected)p.primary else p.onSurfaceVariant,border=BorderStroke(1.dp,if(selected)p.primary else p.outlineVariant),interactionSource=source){Box(Modifier.padding(horizontal=10.dp),contentAlignment=androidx.compose.ui.Alignment.Center){label()}}
}
@Composable fun MotionAssistChip(onClick:()->Unit,label:@Composable ()->Unit,modifier:Modifier=Modifier,enabled:Boolean=true,leadingIcon:(@Composable ()->Unit)?=null) {
 val source=remember{MutableInteractionSource()}
 AssistChip(onClick,label,modifier.pressMotion(source,enabled),enabled=enabled,leadingIcon=leadingIcon,interactionSource=source)
}
@Composable fun RowScope.MotionNavigationBarItem(selected:Boolean,onClick:()->Unit,icon:@Composable ()->Unit,modifier:Modifier=Modifier,label:(@Composable ()->Unit)?=null,alwaysShowLabel:Boolean=true) {
 val source=remember{MutableInteractionSource()}
 NavigationBarItem(selected,onClick,icon,modifier.pressMotion(source),label=label,alwaysShowLabel=alwaysShowLabel,interactionSource=source)
}
