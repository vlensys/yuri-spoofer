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
    private val presetTex = HashMap<String, ClientAsset.ResourceTexture>()  // preset textures
    private val customTex = HashMap<String, ClientAsset.ResourceTexture>()  // custom textures
    private val attempted = HashSet<String>()

    fun enabled(): Boolean =
        SpoofConfig.spoofCape && SpoofConfig.capeId != Capes.NONE && SpoofConfig.capeId != Capes.ADD

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

    // remove cape
    fun removeCape(id: String) {
        val cape = SpoofConfig.customCapes.firstOrNull { it.id == id } ?: return
        SpoofConfig.customCapes.remove(cape)
        customTex.remove(id)
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
