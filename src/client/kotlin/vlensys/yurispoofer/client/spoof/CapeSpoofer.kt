package vlensys.yurispoofer.client.spoof

import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.core.ClientAsset
import net.minecraft.resources.Identifier
import net.minecraft.world.entity.player.PlayerSkin
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

// cape spoofer
object CapeSpoofer {
    const val SHEET_WIDTH = 64
    const val SHEET_HEIGHT = 32
    const val PANEL_X = 1
    const val PANEL_Y = 1
    const val PANEL_WIDTH = 10
    const val PANEL_HEIGHT = 16
    const val MAX_SCALE = 128

    private val presetTex = HashMap<String, ClientAsset.ResourceTexture>()  // preset textures
    private val customTex = HashMap<String, ClientAsset.ResourceTexture>()  // custom textures
    private val customSize = HashMap<String, IntArray>()
    private val attempted = HashSet<String>()

    fun enabled(): Boolean =
        SpoofConfig.masterEnabled && SpoofConfig.spoofCape && SpoofConfig.capeId != Capes.NONE && SpoofConfig.capeId != Capes.ADD

    private fun currentCape(): ClientAsset.ResourceTexture? {
        val id = SpoofConfig.capeId
        return when {
            Capes.isCustom(id) -> loadCustom(id)
            Capes.isPreset(id) -> loadPreset(id)
            else -> null
        }
    }

    @JvmStatic
    fun applyTo(state: AvatarRenderState) {
        if (!enabled()) return
        val cape = currentCape() ?: return
        val skin = state.skin
        state.skin = PlayerSkin.insecure(skin.body(), cape, skin.elytra(), skin.model())
        state.showCape = true
    }

    // gui texture
    @JvmStatic
    fun textureIdFor(id: String): Identifier? = when {
        Capes.isCustom(id) -> loadCustom(id)?.id()
        Capes.isPreset(id) -> loadPreset(id)?.id()
        else -> null
    }

    @JvmStatic
    fun textureSizeFor(id: String): IntArray {
        if (Capes.isCustom(id)) {
            loadCustom(id)
            return customSize[id] ?: intArrayOf(SHEET_WIDTH, SHEET_HEIGHT)
        }
        return intArrayOf(SHEET_WIDTH, SHEET_HEIGHT)
    }

    // preset cape
    private fun loadPreset(id: String): ClientAsset.ResourceTexture? {
        presetTex[id]?.let { return it }
        if (id in attempted) return null
        attempted.add(id)
        val cape = Capes.byId(id) ?: return null
        return try {
            val img = URI("https://textures.minecraft.net/texture/${cape.hash}").toURL()
                .openStream().use { NativeImage.read(it) }
            val texId = Identifier.fromNamespaceAndPath("yuri-spoofer", "cape/$id")
            Minecraft.getInstance().textureManager.register(texId, DynamicTexture({ "yuri-cape-$id" }, img))
            ClientAsset.ResourceTexture(texId, texId).also { presetTex[id] = it }
        } catch (e: Exception) {
            null
        }
    }

    // custom cape
    private fun loadCustom(id: String): ClientAsset.ResourceTexture? {
        customTex[id]?.let { return it }
        val cape = SpoofConfig.customCapes.firstOrNull { it.id == id } ?: return null
        return try {
            val img = Files.newInputStream(Path.of(cape.path)).use { NativeImage.read(it) }
            customSize[id] = intArrayOf(img.width, img.height)
            val texId = Identifier.fromNamespaceAndPath("yuri-spoofer", "cape/${id.replace(':', '_')}")
            Minecraft.getInstance().textureManager.register(texId, DynamicTexture({ "yuri-$id" }, img))
            ClientAsset.ResourceTexture(texId, texId).also { customTex[id] = it }
        } catch (e: Exception) {
            null
        }
    }

    // cape import
    fun importCape(srcPath: String): String? {
        return try {
            val src = Path.of(srcPath)
            Files.newInputStream(src).use { NativeImage.read(it) }   // validate png
            val dir = SpoofConfig.capesDir()
            Files.createDirectories(dir)
            val uid = UUID.randomUUID().toString().substring(0, 8)
            val id = Capes.CUSTOM_PREFIX + uid
            val dest = dir.resolve("$uid.png")
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING)
            val name = src.fileName.toString().substringBeforeLast('.').take(16).ifBlank { "Custom" }
            SpoofConfig.customCapes.add(CustomCape(id, name, dest.toString()))
            SpoofConfig.save()
            loadCustom(id)
            id
        } catch (e: Exception) {
            null
        }
    }

    fun importCapeEdited(srcPath: String, x: Int, y: Int, width: Int, height: Int, scale: Int, rotation: Int): String? {
        val outScale = scale.coerceIn(1, MAX_SCALE)
        val rot = ((rotation % 4) + 4) % 4
        if (width <= 0 || height <= 0) return null
        return try {
            val src = Path.of(srcPath)
            val input = Files.newInputStream(src).use { NativeImage.read(it) }
            val rw = if (rot % 2 == 0) input.width else input.height
            val rh = if (rot % 2 == 0) input.height else input.width
            val outW = SHEET_WIDTH * outScale
            val outH = SHEET_HEIGHT * outScale
            val panelW = PANEL_WIDTH * outScale
            val panelH = PANEL_HEIGHT * outScale
            val out = NativeImage(outW, outH, true)
            for (ty in 0 until outH) {
                for (tx in 0 until outW) {
                    out.setPixel(tx, ty, 0)
                }
            }
            for (ty in 0 until panelH) {
                val sy = ((ty - y).toLong() * rh / height).toInt()
                if (sy !in 0 until rh) continue
                for (tx in 0 until panelW) {
                    val sx = ((tx - x).toLong() * rw / width).toInt()
                    if (sx !in 0 until rw) continue
                    val pixel = rotatedPixel(input, sx, sy, rot)
                    out.setPixel(PANEL_X * outScale + tx, PANEL_Y * outScale + ty, pixel)
                    out.setPixel(12 * outScale + tx, PANEL_Y * outScale + ty, pixel)
                }
            }
            // paint the cape's thin side/top/bottom edges with the image's most-used color
            // so they don't show up as transparent seams against the panels
            val edge = dominantColor(input)
            if (edge != 0) {
                val s = outScale
                fillRect(out, 1 * s, 0, 10 * s, 1 * s, edge)   // top
                fillRect(out, 11 * s, 0, 10 * s, 1 * s, edge)  // bottom
                fillRect(out, 0, 1 * s, 1 * s, 16 * s, edge)   // right edge
                fillRect(out, 11 * s, 1 * s, 1 * s, 16 * s, edge) // left edge
            }
            val dir = SpoofConfig.capesDir()
            Files.createDirectories(dir)
            val uid = UUID.randomUUID().toString().substring(0, 8)
            val id = Capes.CUSTOM_PREFIX + uid
            val dest = dir.resolve("$uid.png")
            out.writeToFile(dest)
            input.close()
            out.close()
            val name = src.fileName.toString().substringBeforeLast('.').take(16).ifBlank { "Custom" }
            SpoofConfig.customCapes.add(CustomCape(id, name, dest.toString()))
            SpoofConfig.save()
            loadCustom(id)
            id
        } catch (e: Exception) {
            null
        }
    }

    // most-used opaque color in the image (channel-order agnostic: the stored pixel value
    // of the most common quantized bucket is reused verbatim, only forced fully opaque)
    private fun dominantColor(img: NativeImage): Int {
        val count = HashMap<Int, Int>()
        val sample = HashMap<Int, Int>()
        var bestKey = 0
        var bestN = 0
        val stride = maxOf(1, maxOf(img.width, img.height) / 256)
        var y = 0
        while (y < img.height) {
            var x = 0
            while (x < img.width) {
                val p = img.getPixel(x, y)
                val a = (p ushr 24) and 0xFF
                if (a >= 128) {
                    val key = p and 0x00F0F0F0
                    val n = (count[key] ?: 0) + 1
                    count[key] = n
                    sample.putIfAbsent(key, p)
                    if (n > bestN) { bestN = n; bestKey = key }
                }
                x += stride
            }
            y += stride
        }
        if (bestN == 0) return 0
        return (sample[bestKey] ?: bestKey) or (0xFF shl 24)
    }

    private fun fillRect(img: NativeImage, x0: Int, y0: Int, w: Int, h: Int, color: Int) {
        val xMax = minOf(x0 + w, img.width)
        val yMax = minOf(y0 + h, img.height)
        var y = y0
        while (y < yMax) {
            var x = x0
            while (x < xMax) { img.setPixel(x, y, color); x++ }
            y++
        }
    }

    private fun rotatedPixel(img: NativeImage, x: Int, y: Int, rotation: Int): Int {
        return when (rotation) {
            1 -> img.getPixel(y, img.height - 1 - x)
            2 -> img.getPixel(img.width - 1 - x, img.height - 1 - y)
            3 -> img.getPixel(img.width - 1 - y, x)
            else -> img.getPixel(x, y)
        }
    }

    fun renameCape(id: String, rawName: String): Boolean {
        val cape = SpoofConfig.customCapes.firstOrNull { it.id == id } ?: return false
        val name = rawName.trim().take(24).ifBlank { "Custom" }
        if (cape.name == name) return true
        cape.name = name
        SpoofConfig.save()
        return true
    }

    // remove cape
    fun removeCape(id: String) {
        val cape = SpoofConfig.customCapes.firstOrNull { it.id == id } ?: return
        SpoofConfig.customCapes.remove(cape)
        customTex.remove(id)
        customSize.remove(id)
        try { Files.deleteIfExists(Path.of(cape.path)) } catch (_: Exception) {}
        if (SpoofConfig.capeId == id) SpoofConfig.capeId = Capes.NONE
        SpoofConfig.save()
    }

    // preload cape
    @JvmStatic
    fun preloadCurrent() {
        val id = SpoofConfig.capeId
        when {
            Capes.isCustom(id) -> loadCustom(id)
            Capes.isPreset(id) -> loadPreset(id)
        }
    }
}
