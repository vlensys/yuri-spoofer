package vlensys.yurispoofer.client.spoof

import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

// head item cache
object HeadItems {
    private val cache = HashMap<String, ItemStack>()

    @JvmStatic
    fun forHash(hash: String): ItemStack = cache.getOrPut(hash) {
        ItemStack(Items.PLAYER_HEAD).also {
            it.set(DataComponents.PROFILE, SkullSpoofer.profileFromHash(hash))
        }
    }
}
