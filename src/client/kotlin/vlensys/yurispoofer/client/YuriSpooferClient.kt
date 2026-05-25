package vlensys.yurispoofer.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.minecraft.client.KeyMapping
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import vlensys.yurispoofer.client.gui.SpooferScreen
import vlensys.yurispoofer.client.spoof.CurrencySpoofer
import vlensys.yurispoofer.client.spoof.LevelSpoofer
import vlensys.yurispoofer.client.spoof.SkillSpoofer
import vlensys.yurispoofer.client.spoof.SlayerSpoofer
import vlensys.yurispoofer.client.spoof.SpoofConfig
import vlensys.yurispoofer.client.spoof.Spoofer

object YuriSpooferClient : ClientModInitializer {
    private val CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("yuri-spoofer", "yuri-spoofer")
    )
    private lateinit var openKey: KeyMapping

    override fun onInitializeClient() {
        SpoofConfig.load()

        openKey = KeyBindingHelper.registerKeyBinding(
            KeyMapping(
                "key.yuri-spoofer.spoofer",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY,
            )
        )

        ClientReceiveMessageEvents.MODIFY_GAME.register(
            ClientReceiveMessageEvents.ModifyGame { message, _ -> Spoofer.spoof(message) }
        )

        ItemTooltipCallback.EVENT.register(
            ItemTooltipCallback { _, _, _, lines ->
                SkillSpoofer.rewriteTooltip(lines)
                SlayerSpoofer.rewriteTooltip(lines)
                CurrencySpoofer.rewriteTooltip(lines)
                LevelSpoofer.rewriteTooltip(lines)
            }
        )

        ClientTickEvents.END_CLIENT_TICK.register(
            ClientTickEvents.EndTick { mc ->
                while (openKey.consumeClick()) {
                    mc.setScreen(SpooferScreen())
                }
            }
        )
    }
}
