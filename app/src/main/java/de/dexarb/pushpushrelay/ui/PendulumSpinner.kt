package de.dexarb.pushpushrelay.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PendulumSpinner(modifier: Modifier = Modifier, size: Dp = 48.dp, pivotOffset: Offset = Offset(0.5f, 0.15f)) {
    val transition = rememberInfiniteTransition(label = "pendulum")
    val angle by transition.animateFloat(
        initialValue = -26f,
        targetValue = 26f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1600
                0f at 0
                26f at 800
                -26f at 1600
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "angle",
    )
    val counterAngle by transition.animateFloat(
        initialValue = 26f,
        targetValue = -26f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1600
                0f at 0
                -26f at 800
                26f at 1600
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "counter",
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val pivot = Offset(w * pivotOffset.x, h * pivotOffset.y)
        val armLength = h * 0.62f
        val bobRadius = w * 0.14f

        rotate(angle, pivot) {
            drawLine(
                color = BrandPeach,
                start = pivot,
                end = Offset(pivot.x, pivot.y + armLength),
                strokeWidth = w * 0.05f,
                cap = StrokeCap.Round,
            )
            drawCircle(BrandPeach, radius = bobRadius, center = Offset(pivot.x, pivot.y + armLength + bobRadius))
            drawCircle(
                color = BrandPeach.copy(alpha = 0.5f),
                radius = bobRadius * 0.7f,
                center = Offset(pivot.x, pivot.y + armLength + bobRadius),
                style = Stroke(width = w * 0.02f),
            )
        }
        rotate(counterAngle, pivot) {
            val ghost = Offset(pivot.x, pivot.y + armLength + bobRadius)
            drawCircle(BrandPeach.copy(alpha = 0.15f), radius = bobRadius, center = ghost)
        }
        drawCircle(Ink.copy(alpha = 0.35f), radius = w * 0.04f, center = pivot)
    }
}
