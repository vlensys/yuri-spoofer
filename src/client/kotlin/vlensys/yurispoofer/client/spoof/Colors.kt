package vlensys.yurispoofer.client.spoof

// color helpers
object Colors {

    // rgb
    @JvmStatic
    fun hsvToRgb(h: Float, s: Float, v: Float): Int {
        val hh = ((h % 1f) + 1f) % 1f * 6f
        val i = hh.toInt()
        val f = hh - i
        val p = v * (1f - s)
        val q = v * (1f - s * f)
        val t = v * (1f - s * (1f - f))
        val (r, g, b) = when (i) {
            0 -> Triple(v, t, p)
            1 -> Triple(q, v, p)
            2 -> Triple(p, v, t)
            3 -> Triple(p, q, v)
            4 -> Triple(t, p, v)
            else -> Triple(v, p, q)
        }
        val ri = (r * 255f + 0.5f).toInt().coerceIn(0, 255)
        val gi = (g * 255f + 0.5f).toInt().coerceIn(0, 255)
        val bi = (b * 255f + 0.5f).toInt().coerceIn(0, 255)
        return (ri shl 16) or (gi shl 8) or bi
    }

    // argb
    @JvmStatic
    fun hsvToArgb(h: Float, s: Float, v: Float): Int = 0xFF000000.toInt() or hsvToRgb(h, s, v)

    // hsv
    @JvmStatic
    fun rgbToHsv(rgb: Int): FloatArray {
        val r = (rgb shr 16 and 0xFF) / 255f
        val g = (rgb shr 8 and 0xFF) / 255f
        val b = (rgb and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val d = max - min
        val v = max
        val s = if (max == 0f) 0f else d / max
        var h = 0f
        if (d != 0f) {
            h = when (max) {
                r -> ((g - b) / d) % 6f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            } / 6f
            if (h < 0f) h += 1f
        }
        return floatArrayOf(h, s, v)
    }
}
