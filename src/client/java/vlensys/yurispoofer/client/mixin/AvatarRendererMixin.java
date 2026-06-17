package vlensys.yurispoofer.client.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import vlensys.yurispoofer.client.spoof.ArmorSpoofer;
import vlensys.yurispoofer.client.spoof.CapeSpoofer;
import vlensys.yurispoofer.client.spoof.SkullSpoofer;

// avatar render spoof
@Mixin(AvatarRenderer.class)
public class AvatarRendererMixin {
    private static boolean yuri$loggedError = false;

    @Inject(
        method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
        at = @At("TAIL")
    )
    private void yuri$spoofAvatar(Avatar entity, AvatarRenderState state, float partialTicks, CallbackInfo ci) {
        if (entity != Minecraft.getInstance().player) return;
        // render guard
        try {
            CapeSpoofer.applyTo(state);
            SkullSpoofer.applyTo(state);
            ArmorSpoofer.applyTo(state);
        } catch (Throwable t) {
            if (!yuri$loggedError) {
                yuri$loggedError = true;
                System.err.println("[yuri-spoofer] appearance spoof failed (further errors suppressed): " + t);
                t.printStackTrace();
            }
        }
    }
}
