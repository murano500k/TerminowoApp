package com.stc.terminowo.presentation.consent

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.IntSize
import com.stc.terminowo.platform.ErrorReporter
import com.stc.terminowo.platform.decodeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.float
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.ExperimentalResourceApi
import terminowo.shared.generated.resources.Res
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.ceil
import kotlin.math.max

/**
 * Plays the tile animation from the Lottie files natively. The animation is static logos over
 * image tiles that scroll up linearly in a seamless loop, so the tiles are drawn once into a
 * cached layer and only that layer moves. Rendering it through the Lottie player redrew 80+
 * image layers every display refresh and tripped iOS CPU limits.
 */
@OptIn(ExperimentalResourceApi::class)
@Composable
fun ConsentBackground() {
    val path = if (isSystemInDarkTheme()) {
        "files/terminowo_tile_dark.json"
    } else {
        "files/terminowo_tile_light.json"
    }

    var scene by remember { mutableStateOf<TileScene?>(null) }
    LaunchedEffect(path) {
        scene = withContext(Dispatchers.Default) {
            runCatching { parseTileScene(Res.readBytes(path).decodeToString()) }
                .onFailure { ErrorReporter.capture(it) }
                .getOrNull()
        }
    }
    val current = scene ?: return

    val tilesLayer = rememberGraphicsLayer()
    // Plain holder: the layer is re-recorded from draw, and a state read there would loop.
    val recordedFor = remember { arrayOfNulls<Pair<TileScene, IntSize>>(1) }
    var progress by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(current) {
        val startNanos = withFrameNanos { it }
        while (true) {
            withFrameNanos { now ->
                val seconds = (now - startNanos) / 1_000_000_000.0
                progress = ((seconds / current.durationSeconds) % 1.0).toFloat()
            }
            // ~30 fps, the rate the animation was made at; every vsync redraws the whole screen.
            delay(25)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
            .drawBehind {
                val scale = max(size.width / current.width, size.height / current.height)
                val origin = Offset(
                    (size.width - current.width * scale) / 2,
                    (size.height - current.height * scale) / 2
                )
                val viewSize = IntSize(size.width.toInt(), size.height.toInt())
                if (recordedFor[0] != current to viewSize) {
                    val extent = current.tilesExtent
                    tilesLayer.record(
                        size = IntSize(
                            size.width.toInt(),
                            ceil((extent.second - extent.first) * scale).toInt()
                        )
                    ) {
                        translate(origin.x, -extent.first * scale) {
                            scale(scale, pivot = Offset.Zero) { current.tiles.forEach { drawSprite(it) } }
                        }
                    }
                    recordedFor[0] = current to viewSize
                }
                translate(0f, origin.y + (current.tilesExtent.first - current.loopDistance * progress) * scale) {
                    drawLayer(tilesLayer)
                }
                translate(origin.x, origin.y) {
                    scale(scale, pivot = Offset.Zero) { current.logos.forEach { drawSprite(it) } }
                }
            }
    )
}

private class Sprite(val image: ImageBitmap, val size: IntSize, val x: Float, val y: Float, val alpha: Float)

private class TileScene(
    val width: Float,
    val height: Float,
    val durationSeconds: Double,
    val loopDistance: Float,
    /** Bottom-most layers first, positioned at the start of the loop. */
    val tiles: List<Sprite>,
    val logos: List<Sprite>,
) {
    /** Vertical range covered by the tiles at the start of the loop, in composition units. */
    val tilesExtent: Pair<Float, Float> = tiles.minOf { it.y } to tiles.maxOf { it.y + it.size.height }
}

private fun DrawScope.drawSprite(sprite: Sprite) {
    translate(sprite.x, sprite.y) {
        drawImage(sprite.image, dstSize = sprite.size, alpha = sprite.alpha)
    }
}

@OptIn(ExperimentalEncodingApi::class)
private fun parseTileScene(json: String): TileScene {
    val root = Json.parseToJsonElement(json).jsonObject
    fun JsonObject.num(key: String) = getValue(key).jsonPrimitive.float
    fun JsonArray.num(i: Int) = get(i).jsonPrimitive.float

    val images = root.getValue("assets").jsonArray.associate { element ->
        val asset = element.jsonObject
        val data = asset.getValue("p").jsonPrimitive.content.substringAfter("base64,")
        val image = requireNotNull(decodeImageBitmap(Base64.decode(data))) { "Undecodable tile image" }
        asset.getValue("id").jsonPrimitive.content to
            (image to IntSize(asset.num("w").toInt(), asset.num("h").toInt()))
    }

    val tiles = mutableListOf<Sprite>()
    val logos = mutableListOf<Sprite>()
    var loopDistance = 0f
    // Lottie lists layers top-most first.
    for (element in root.getValue("layers").jsonArray.reversed()) {
        val layer = element.jsonObject
        val transform = layer.getValue("ks").jsonObject
        val anchor = transform.getValue("a").jsonObject.getValue("k").jsonArray
        val opacity = transform.getValue("o").jsonObject.num("k") / 100f
        val position = transform.getValue("p").jsonObject
        val (image, size) = images.getValue(layer.getValue("refId").jsonPrimitive.content)
        if (position.getValue("a").jsonPrimitive.content == "1") {
            val keyframe = position.getValue("k").jsonArray[0].jsonObject
            val start = keyframe.getValue("s").jsonArray
            loopDistance = start.num(1) - keyframe.getValue("e").jsonArray.num(1)
            tiles += Sprite(image, size, start.num(0) - anchor.num(0), start.num(1) - anchor.num(1), opacity)
        } else {
            val at = position.getValue("k").jsonArray
            logos += Sprite(image, size, at.num(0) - anchor.num(0), at.num(1) - anchor.num(1), opacity)
        }
    }
    val frameRate = root.num("fr")
    return TileScene(
        width = root.num("w"),
        height = root.num("h"),
        durationSeconds = ((root.num("op") - root.num("ip")) / frameRate).toDouble(),
        loopDistance = loopDistance,
        tiles = tiles,
        logos = logos,
    )
}
