package vlensys.yurispoofer.client.mixin;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vlensys.yurispoofer.client.spoof.Spoofer;

@Mixin(PlayerTeam.class)
public class PlayerTeamMixin {
    @Inject(method = "formatNameForTeam", at = @At("RETURN"), cancellable = true)
    private static void yuri$spoofTeamName(Team team, Component name, CallbackInfoReturnable<MutableComponent> cir) {
        MutableComponent original = cir.getReturnValue();
        if (original == null) return;
        Component spoofed = Spoofer.spoof(original);
        if (spoofed != original) cir.setReturnValue(spoofed.copy());
    }
}
