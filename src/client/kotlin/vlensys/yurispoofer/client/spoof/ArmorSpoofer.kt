package vlensys.yurispoofer.client.spoof

import net.minecraft.client.renderer.entity.state.HumanoidRenderState
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.component.DyedItemColor
import net.minecraft.world.item.equipment.trim.ArmorTrim

// armor spoofer
object ArmorSpoofer {
    const val HEAD = "HEAD"
    const val CHEST = "CHEST"
    const val LEGS = "LEGS"
    const val FEET = "FEET"
    val SLOTS = listOf(HEAD, CHEST, LEGS, FEET)

    fun enabled(): Boolean = SpoofConfig.spoofArmor

    @JvmStatic
    fun applyTo(state: HumanoidRenderState) {
        if (!enabled()) return
        for (slot in SLOTS) {
            // skull blocks helmet
            if (slot == HEAD && SkullSpoofer.enabled()) continue
            val piece = SpoofConfig.armorPieces[slot] ?: continue
            if (!piece.on) continue
            val stack = buildStack(slot, piece) ?: continue
            when (slot) {
                HEAD -> state.headEquipment = stack
                CHEST -> state.chestEquipment = stack
                LEGS -> state.legsEquipment = stack
                FEET -> state.feetEquipment = stack
            }
        }
    }

    private fun itemFor(slot: String) = when (slot) {
        HEAD -> Items.LEATHER_HELMET
        CHEST -> Items.LEATHER_CHESTPLATE
        LEGS -> Items.LEATHER_LEGGINGS
        FEET -> Items.LEATHER_BOOTS
        else -> Items.LEATHER_CHESTPLATE
    }

    // preview stack
    @JvmStatic
    fun previewStack(slot: String, color: Int, pattern: String, material: String): ItemStack {
        val stack = ItemStack(itemFor(slot))
        stack.set(DataComponents.DYED_COLOR, DyedItemColor(color and 0xFFFFFF))
        if (pattern.isNotEmpty() && material.isNotEmpty()) {
            val mat = Trims.materialHolder(material)
            val pat = Trims.patternHolder(pattern)
            if (mat != null && pat != null) stack.set(DataComponents.TRIM, ArmorTrim(mat, pat))
        }
        return stack
    }

    private fun buildStack(slot: String, piece: ArmorPiece): ItemStack? {
        val stack = ItemStack(itemFor(slot))
        stack.set(DataComponents.DYED_COLOR, DyedItemColor(piece.color and 0xFFFFFF))
        if (piece.trimPattern.isNotEmpty() && piece.trimMaterial.isNotEmpty()) {
            val mat = Trims.materialHolder(piece.trimMaterial)
            val pat = Trims.patternHolder(piece.trimPattern)
            if (mat != null && pat != null) {
                stack.set(DataComponents.TRIM, ArmorTrim(mat, pat))
            }
        }
        return stack
    }
}

// armor config
data class ArmorPiece(
    var on: Boolean = false,
    var color: Int = 0xFFFFFF,
    var trimPattern: String = "",
    var trimMaterial: String = "",
)
