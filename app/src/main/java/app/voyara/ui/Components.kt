package app.voyara.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private val PinLift = 14.dp

/**
 * Map pin, 40 × 52 dp, with its tip at the bottom-centre of its bounds. The ground shadow stays
 * put while the head lifts, which is how a dragged pin reads as picked up.
 */
@Composable
fun PinGlyph(lifted: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val lift by animateDpAsState(
        if (lifted) PinLift else 0.dp,
        spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMediumLow),
        label = "lift",
    )
    Box(modifier.size(40.dp, 52.dp)) {
        Canvas(Modifier.align(Alignment.BottomCenter).offset(y = 3.dp).size(16.dp, 6.dp)) {
            drawOval(Color.Black.copy(alpha = 0.3f - 0.15f * (lift / PinLift)))
        }
        Canvas(Modifier.fillMaxSize().offset { IntOffset(0, -lift.roundToPx()) }) {
            val w = size.width
            val h = size.height
            val r = w / 2
            val pin = Path().apply {
                moveTo(r, h)
                cubicTo(r - r * 0.35f, h - r * 0.55f, 0f, r * 1.45f, 0f, r)
                arcTo(Rect(0f, 0f, w, w), 180f, 180f, false)
                cubicTo(w, r * 1.45f, r + r * 0.35f, h - r * 0.55f, r, h)
                close()
            }
            drawPath(pin, colors.primary)
            drawPath(pin, colors.surfaceContainerLowest, style = Stroke(width = 2.dp.toPx()))
            drawCircle(colors.onPrimary, radius = r * 0.36f, center = Offset(r, r))
        }
    }
}

/** Live indicator. Compose already freezes it when the system's animator scale is 0. */
@Composable
fun PulsingDot(color: Color, modifier: Modifier = Modifier) {
    val pulse = rememberInfiniteTransition(label = "pulse")
    val scale by pulse.animateFloat(1f, 2.4f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "scale")
    val alpha by pulse.animateFloat(0.55f, 0f, infiniteRepeatable(tween(1400), RepeatMode.Restart), label = "alpha")
    Box(modifier.size(22.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(10.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .background(color, CircleShape),
        )
        Box(Modifier.size(10.dp).background(color, CircleShape))
    }
}

// ~0.3 s settle, no overshoot.
private fun <T> iconSpring(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 440f)

/** Swaps icons with a cross-fade: scale 0.25 → 1, opacity 0 → 1, blur 4 → 0 dp. */
@Composable
fun IconSwap(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    AnimatedContent(
        targetState = icon,
        transitionSpec = {
            (fadeIn(iconSpring()) + scaleIn(iconSpring(), initialScale = 0.25f))
                .togetherWith(fadeOut(iconSpring()) + scaleOut(iconSpring(), targetScale = 0.25f))
                .using(SizeTransform(clip = false))
        },
        label = "icon",
    ) { target ->
        val blur by transition.animateDp(transitionSpec = { iconSpring() }, label = "blur") {
            if (it == EnterExitState.Visible) 0.dp else 4.dp
        }
        Icon(target, contentDescription, modifier.blur(blur, BlurredEdgeTreatment.Unbounded), tint = tint)
    }
}

/** Tactile press feedback: scales to exactly 0.96 while pressed. */
@Composable
fun pressScale(source: InteractionSource): Modifier {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, tween(150), label = "press")
    return Modifier.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
