package vlensys.yurispoofer.client

import com.mojang.blaze3d.platform.InputConstants
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents
import net.fabricmc.fabric.api.client.screen.v1.Screens
import net.minecraft.client.KeyMapping
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.resources.Identifier
import org.lwjgl.glfw.GLFW
import vlensys.yurispoofer.client.gui.AppearanceEditorScreen
import vlensys.yurispoofer.client.gui.PaintbrushButton
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

        openKey = KeyMappingHelper.registerKeyMapping(
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

        // inventory button
        ScreenEvents.AFTER_INIT.register(ScreenEvents.AfterInit { client, screen, sw, sh ->
            if (screen is InventoryScreen) {
                val left = (sw - 176) / 2
                val top = (sh - 166) / 2
                Screens.getWidgets(screen).add(
                    PaintbrushButton(left + 176 - 21, top + 5, 18) { client.setScreen(AppearanceEditorScreen()) }
                )
            }
        })

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
                    mc.setScreen(AppearanceEditorScreen())
                }
            }
        )
    }
}
