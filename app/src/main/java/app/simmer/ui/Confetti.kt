package app.simmer.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

/** A light shower of confetti that loops while shown. Pure drawing, no library. */
@Composable
fun Confetti(modifier: Modifier = Modifier, colors: List<Color>) {
    data class Bit(val x: Float, val delay: Float, val speed: Float, val size: Float, val color: Color, val spin: Float)
    val bits = remember {
        val rnd = Random(7)
        List(70) { Bit(rnd.nextFloat(), rnd.nextFloat(), 0.6f + rnd.nextFloat() * 0.8f, 6f + rnd.nextFloat() * 8f, colors[rnd.nextInt(colors.size)], rnd.nextFloat() * 360f) }
    }
    val t by rememberInfiniteTransition(label = "confetti").animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "t")
    Canvas(modifier.fillMaxSize()) {
        bits.forEach { b ->
            val p = ((t * b.speed) + b.delay) % 1f
            val y = p * size.height * 1.1f - size.height * 0.05f
            val x = b.x * size.width + kotlin.math.sin((p * 6f + b.delay) * Math.PI).toFloat() * 24f
            rotate(b.spin + p * 540f, pivot = Offset(x, y)) {
                drawRect(b.color, topLeft = Offset(x - b.size / 2, y - b.size / 2), size = Size(b.size, b.size * 0.6f))
            }
        }
    }
}
