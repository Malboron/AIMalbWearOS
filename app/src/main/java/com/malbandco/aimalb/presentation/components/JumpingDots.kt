package com.malbandco.aimalb.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun JumpingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "JumpingDots")
    
    val dotSize = 8.dp
    val spacing = 4.dp
    val jumpHeight = 10.dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        (0..2).forEach { index ->
            val yOffset by transition.animateFloat(
                initialValue = 0f,
                targetValue = -jumpHeight.value,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 300, easing = LinearEasing, delayMillis = index * 100),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "Dot $index"
            )

            Box(
                modifier = Modifier
                    .size(dotSize)
                    .offset(y = yOffset.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
