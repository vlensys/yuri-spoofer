package vlensys.yurispoofer.client.spoof

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// color wheel
object ColorWheel {
    @JvmField val TEX_ID: Identifier = Identifier.fromNamespaceAndPath("yuri-spoofer", "armor/wheel")
    const val SIZE = 96

    @Volatile private var built = false

    @JvmStatic
    fun ensureBuilt() {
        if (built) return
        val img = NativeImage(SIZE, SIZE, true)
        val r = SIZE / 2f
        for (y in 0 until SIZE) {
            for (x in 0 until SIZE) {
                val dx = (x + 0.5f) - r
                val dy = (y + 0.5f) - r
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= r - 0.5f) {
                    val hue = ((atan2(dy, dx) / (2.0 * Math.PI)).toFloat() + 1f) % 1f
                    val sat = (dist / r).coerceIn(0f, 1f)
                    img.setPixel(x, y, Colors.hsvToArgb(hue, sat, 1f))
                } else {
                    img.setPixel(x, y, 0)
                }
            }
        }
        Minecraft.getInstance().textureManager.register(TEX_ID, DynamicTexture({ "yuri-armor-wheel" }, img))
        built = true
    }

    // color pick
    @JvmStatic
    fun pick(px: Float, py: Float, size: Int): FloatArray? {
        val r = size / 2f
        val dx = px - r
        val dy = py - r
        val dist = sqrt(dx * dx + dy * dy)
        if (dist > r) return null
        val hue = ((atan2(dy, dx) / (2.0 * Math.PI)).toFloat() + 1f) % 1f
        val sat = (dist / r).coerceIn(0f, 1f)
        return floatArrayOf(hue, sat)
    }

    // color marker
    @JvmStatic
    fun markerPos(hue: Float, sat: Float, size: Int): IntArray {
        val r = size / 2f
        val ang = hue * 2.0 * Math.PI
        val mx = r + sat * r * cos(ang).toFloat()
        val my = r + sat * r * sin(ang).toFloat()
        return intArrayOf(mx.toInt(), my.toInt())
    }
}
