package vlensys.yurispoofer.client.spoof

import com.google.common.collect.LinkedHashMultimap
import com.mojang.authlib.GameProfile
import com.mojang.authlib.properties.Property
import com.mojang.authlib.properties.PropertyMap
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState
import net.minecraft.client.renderer.texture.DynamicTexture
import net.minecraft.resources.Identifier
import net.minecraft.world.item.component.ResolvableProfile
import net.minecraft.world.level.block.SkullBlock
import java.net.URI
import java.util.Base64
import java.util.UUID

// skull spoofer
object SkullSpoofer {
    private val PREVIEW_ID: Identifier = Identifier.fromNamespaceAndPath("yuri-spoofer", "skull/preview")

    private var cachedKey: String? = null
    private var cachedProfile: ResolvableProfile? = null

    private var previewHash: String? = null
    @Volatile private var previewReady = false

    fun enabled(): Boolean = SpoofConfig.spoofSkull

    @JvmStatic
    fun applyTo(state: LivingEntityRenderState) {
        if (!enabled()) return
        val profile = currentProfile() ?: return
        state.wornHeadType = SkullBlock.Types.PLAYER
        state.wornHeadProfile = profile
    }

    // current spoof profile
    @JvmStatic
    fun isSpoofProfile(profile: ResolvableProfile?): Boolean =
        enabled() && profile != null && profile === cachedProfile

    private fun currentProfile(): ResolvableProfile? {
        val key = SpoofConfig.skullId + "|" + SpoofConfig.skullCustomTexture
        if (key == cachedKey) return cachedProfile
        cachedKey = key
        val value = textureValue()
        cachedProfile = if (value == null) null else buildProfile(value, key)
        return cachedProfile
    }

    // texture profile
    fun buildProfile(value: String, seed: String): ResolvableProfile {
        // authlib properties
        val props = LinkedHashMultimap.create<String, Property>()
        props.put("textures", Property("textures", value))
        val gp = GameProfile(UUID.nameUUIDFromBytes(seed.toByteArray()), "yurihead", PropertyMap(props))
        return ResolvableProfile.createResolved(gp)
    }

    // head profile
    fun profileFromHash(hash: String): ResolvableProfile = buildProfile(base64FromHash(hash), "h:$hash")

    // texture value
    private fun textureValue(): String? {
        if (SpoofConfig.skullId == Skulls.CUSTOM) {
            val raw = SpoofConfig.skullCustomTexture.trim()
            return when {
                raw.isEmpty() -> null
                // base64 value
                raw.length > 100 && !raw.contains('/') -> raw
                else -> base64FromHash(extractHash(raw))
            }
        }
        val head = Skulls.byId(SpoofConfig.skullId) ?: return null
        return base64FromHash(head.tex)
    }

    private fun extractHash(s: String): String = s.substringAfterLast('/').trim()

    private fun base64FromHash(hash: String): String {
        val json = """{"textures":{"SKIN":{"url":"http://textures.minecraft.net/texture/$hash"}}}"""
        return Base64.getEncoder().encodeToString(json.toByteArray(Charsets.UTF_8))
    }

    // gui preview

    // preview texture
    @JvmStatic
    fun previewTextureId(): Identifier = PREVIEW_ID

    @JvmStatic
    fun previewReady(): Boolean = previewReady

    // load preview
    @JvmStatic
    fun loadPreview(hash: String?) {
        if (hash.isNullOrEmpty()) { previewReady = false; previewHash = null; return }
        if (hash == previewHash && previewReady) return
        previewHash = hash
        previewReady = false
        try {
            val url = URI("https://textures.minecraft.net/texture/$hash").toURL()
            val img = url.openStream().use { NativeImage.read(it) }
            Minecraft.getInstance().textureManager.register(PREVIEW_ID, DynamicTexture({ "yuri-skull-preview" }, img))
            previewReady = true
        } catch (_: Exception) {
            previewReady = false
        }
    }
}
