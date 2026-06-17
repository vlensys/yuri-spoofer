package vlensys.yurispoofer.client.mixin;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import vlensys.yurispoofer.client.spoof.Spoofer;

// text hook
@Mixin(GuiGraphicsExtractor.class)
public class GuiGraphicsMixin {
    @ModifyVariable(
        method = "text(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",
        at = @At("HEAD"),
        argsOnly = true
    )
    private Component yuri$spoofText(Component text) {
        return text == null ? null : Spoofer.spoof(text);
    }
}
