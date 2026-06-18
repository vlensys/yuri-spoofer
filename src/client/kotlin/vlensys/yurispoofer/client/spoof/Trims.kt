package vlensys.yurispoofer.client.spoof

import net.minecraft.client.Minecraft
import net.minecraft.core.Holder
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.core.registries.Registries
import net.minecraft.resources.Identifier
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.equipment.trim.TrimMaterial
import net.minecraft.world.item.equipment.trim.TrimMaterials
import net.minecraft.world.item.equipment.trim.TrimPattern
import net.minecraft.world.item.equipment.trim.TrimPatterns

// trims
object Trims {
    const val NONE = ""

    val MATERIAL_KEYS: List<ResourceKey<TrimMaterial>> = listOf(
        TrimMaterials.QUARTZ, TrimMaterials.IRON, TrimMaterials.GOLD, TrimMaterials.COPPER,
        TrimMaterials.NETHERITE, TrimMaterials.REDSTONE, TrimMaterials.EMERALD, TrimMaterials.DIAMOND,
        TrimMaterials.LAPIS, TrimMaterials.AMETHYST, TrimMaterials.RESIN,
    )

    val PATTERN_KEYS: List<ResourceKey<TrimPattern>> = listOf(
        TrimPatterns.SENTRY, TrimPatterns.VEX, TrimPatterns.WILD, TrimPatterns.COAST, TrimPatterns.DUNE,
        TrimPatterns.WARD, TrimPatterns.EYE, TrimPatterns.TIDE, TrimPatterns.SNOUT, TrimPatterns.RIB,
        TrimPatterns.SPIRE, TrimPatterns.WAYFINDER, TrimPatterns.SHAPER, TrimPatterns.SILENCE,
        TrimPatterns.RAISER, TrimPatterns.HOST, TrimPatterns.FLOW, TrimPatterns.BOLT,
    )

    val MATERIAL_IDS: List<String> = MATERIAL_KEYS.map { it.identifier().path }
    val PATTERN_IDS: List<String> = PATTERN_KEYS.map { it.identifier().path }

    private val materialByid: Map<String, ResourceKey<TrimMaterial>> = MATERIAL_KEYS.associateBy { it.identifier().path }
    private val patternById: Map<String, ResourceKey<TrimPattern>> = PATTERN_KEYS.associateBy { it.identifier().path }

    fun display(id: String): String =
        if (id.isEmpty()) "None" else id.replaceFirstChar { it.uppercase() }

    // ingredient item per trim material (for the gui icon)
    private val MATERIAL_ITEM: Map<String, String> = mapOf(
        "quartz" to "quartz",
        "iron" to "iron_ingot",
        "gold" to "gold_ingot",
        "copper" to "copper_ingot",
        "netherite" to "netherite_ingot",
        "redstone" to "redstone",
        "emerald" to "emerald",
        "diamond" to "diamond",
        "lapis" to "lapis_lazuli",
        "amethyst" to "amethyst_shard",
        "resin" to "resin_brick",
    )

    // gui icon that actually matches the selected trim pattern (its smithing template)
    fun patternIcon(id: String): ItemStack =
        if (id.isEmpty()) ItemStack.EMPTY else itemById("${id}_armor_trim_smithing_template")

    // gui icon that actually matches the selected trim material (its ingredient)
    fun materialIcon(id: String): ItemStack =
        if (id.isEmpty()) ItemStack.EMPTY else itemById(MATERIAL_ITEM[id] ?: return ItemStack.EMPTY)

    private fun itemById(path: String): ItemStack =
        ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("minecraft", path)))

    fun materialHolder(id: String): Holder<TrimMaterial>? {
        val key = materialByid[id] ?: return null
        val ra = Minecraft.getInstance().connection?.registryAccess() ?: return null
        return runCatching { ra.lookupOrThrow(Registries.TRIM_MATERIAL).getOrThrow(key) }.getOrNull()
    }

    fun patternHolder(id: String): Holder<TrimPattern>? {
        val key = patternById[id] ?: return null
        val ra = Minecraft.getInstance().connection?.registryAccess() ?: return null
        return runCatching { ra.lookupOrThrow(Registries.TRIM_PATTERN).getOrThrow(key) }.getOrNull()
    }
}
