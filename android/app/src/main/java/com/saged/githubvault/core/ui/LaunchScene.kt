package com.saged.githubvault.core.ui

import android.animation.ValueAnimator
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Opening scene for GitHub Vault.
 *
 * It is intentionally a single, calm screen: one brand mark, one clear headline and an honest
 * progress line. The real app is already loading underneath, so finishing the reveal feels quick
 * and nothing has to be tapped. Tapping anywhere or "تخطّى" ends the scene immediately.
 */
@Composable
fun LaunchScene(content: @Composable () -> Unit) {
    var finished by rememberSaveable { mutableStateOf(!ValueAnimator.areAnimatorsEnabled()) }
    val reveal = remember { Animatable(if (finished) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!finished) {
            reveal.animateTo(1f, tween(VisualPolicy.OPENING_MS, easing = FastOutSlowInEasing))
            finished = true
        }
    }
    BackHandler(!finished) { finished = true }
    val progress = if (finished) 1f else reveal.value

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                alpha = .55f + .45f * progress
                scaleX = .985f + .015f * progress
                scaleY = scaleX
            }
        ) { content() }

        AnimatedVisibility(
            visible = !finished,
            enter = EnterTransition.None,
            exit = fadeOut(tween(280))
        ) {
            LaunchArtwork(progress = progress, onFinish = { finished = true })
        }
    }
}

@Composable
private fun LaunchArtwork(progress: Float, onFinish: () -> Unit) {
    val palette = MaterialTheme.colorScheme
    val dark = palette.background.luminance() < .4f
    val canvas = if (dark) Color(0xFF081612) else Color(0xFFFbfDFb)
    val canvasEnd = if (dark) Color(0xFF10271F) else Color(0xFFE9F4EE)
    val glow = if (dark) palette.primary.copy(alpha = .20f) else palette.primary.copy(alpha = .13f)
    val enter = ((progress - .03f) / .48f).coerceIn(0f, 1f)
    val controls = ((progress - .58f) / .42f).coerceIn(0f, 1f)
    val interaction = remember { MutableInteractionSource() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(canvas, palette.background, canvasEnd)))
            .clickable(interactionSource = interaction, indication = null, onClick = onFinish)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val shortest = size.minDimension
            drawCircle(
                color = glow,
                radius = shortest * .62f,
                center = Offset(size.width * .86f, size.height * .14f)
            )
            drawCircle(
                color = palette.primary.copy(alpha = if (dark) .07f else .05f),
                radius = shortest * .52f,
                center = Offset(size.width * .08f, size.height * .9f)
            )
        }

        Row(
            Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandMark(34.dp)
            Spacer(Modifier.width(11.dp))
            Text(
                "GitHub Vault",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = palette.onSurface
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(palette.primary.copy(alpha = if (dark) .16f else .09f))
                    .padding(horizontal = 11.dp, vertical = 5.dp)
            ) {
                Text(
                    "0.7",
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Column(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 32.dp)
                .graphicsLayer {
                    alpha = enter
                    translationY = (1f - enter) * 20.dp.toPx()
                    scaleX = .94f + .06f * enter
                    scaleY = scaleX
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BrandMark(size = 92.dp, prominent = true)
            Spacer(Modifier.height(30.dp))
            Text(
                "ملفاتك،\nدائمًا معك.",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 34.sp,
                    lineHeight = 44.sp,
                    letterSpacing = (-0.3f).sp
                ),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = palette.onSurface
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "كل مستودعاتك وملفاتك.",
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 23.sp),
                color = palette.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .fillMaxWidth()
                .padding(start = 22.dp, end = 22.dp, bottom = 24.dp)
                .graphicsLayer {
                    alpha = controls
                    translationY = (1f - controls) * 12.dp.toPx()
                }
        ) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = palette.surface.copy(alpha = if (dark) .92f else .96f),
                tonalElevation = 0.dp,
                shadowElevation = if (dark) 0.dp else 3.dp,
                border = BorderStroke(1.dp, palette.outlineVariant.copy(alpha = .55f))
            ) {
                Column(Modifier.padding(start = 18.dp, end = 8.dp, top = 8.dp, bottom = 16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (progress > .78f) "جاهزين" else "نجهّز مساحتك",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.onSurface
                            )
                            Text(
                                "ما تحتاج شيء.",
                                style = MaterialTheme.typography.labelSmall,
                                color = palette.onSurfaceVariant
                            )
                        }
                        TextButton(onClick = onFinish) {
                            Text("تخطّى", color = palette.primary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(end = 10.dp)
                            .height(5.dp)
                            .clip(CircleShape)
                            .background(palette.primary.copy(alpha = if (dark) .18f else .12f))
                    ) {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(progress.coerceIn(0f, 1f))
                                .clip(CircleShape)
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(palette.primary, palette.secondary)
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

/** A simple folder tile: the whole identity is built from one shape instead of a busy illustration. */
@Composable
internal fun BrandMark(size: Dp, prominent: Boolean = false) {
    val palette = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(size)
            .graphicsLayer { rotationZ = if (prominent) -4f else 0f },
        contentAlignment = Alignment.Center
    ) {
        if (prominent) {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = 9.dp, y = 9.dp)
                    .clip(RoundedCornerShape(size * .31f))
                    .background(palette.primary.copy(alpha = .14f))
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(size * .30f))
                .background(Brush.linearGradient(listOf(palette.primary, palette.secondary)))
                .border(
                    width = 1.dp,
                    color = palette.onPrimary.copy(alpha = .18f),
                    shape = RoundedCornerShape(size * .30f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.FolderOpen,
                contentDescription = null,
                tint = palette.onPrimary,
                modifier = Modifier.size(size * .44f)
            )
        }
    }
}
