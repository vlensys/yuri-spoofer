package vlensys.yurispoofer.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CustomHeadLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.SkullBlock;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import vlensys.yurispoofer.client.spoof.SkullSpoofer;

// skull cutout
@Mixin(CustomHeadLayer.class)
public class CustomHeadLayerMixin {
    @Shadow @Final private PlayerSkinRenderCache playerSkinRenderCache;

    @Inject(
        method = "resolveSkullRenderType(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lnet/minecraft/world/level/block/SkullBlock$Type;)Lnet/minecraft/client/renderer/rendertype/RenderType;",
        at = @At("HEAD"),
        cancellable = true
    )
    private void yuri$cutoutSpoofedHead(LivingEntityRenderState state, SkullBlock.Type type, CallbackInfoReturnable<RenderType> cir) {
        if (type != SkullBlock.Types.PLAYER) return;
        ResolvableProfile profile = state.wornHeadProfile;
        if (!SkullSpoofer.isSpoofProfile(profile)) return;
        Identifier texture = this.playerSkinRenderCache.getOrDefault(profile).playerSkin().body().texturePath();
        cir.setReturnValue(RenderTypes.entityCutoutZOffset(texture));
    }

    // skull scale
    @Inject(method = "submit", require = 0, at = @At(value = "INVOKE",
        target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V", ordinal = 1, shift = At.Shift.AFTER))
    private void yuri$enlargeSpoofedHead(PoseStack poseStack, SubmitNodeCollector collector, int light,
                                         LivingEntityRenderState state, float yRot, float xRot, CallbackInfo ci) {
        if (SkullSpoofer.isSpoofProfile(state.wornHeadProfile)) {
            poseStack.scale(1.06F, 1.06F, 1.06F);
        }
    }
}
