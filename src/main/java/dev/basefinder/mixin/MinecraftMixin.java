package dev.basefinder.mixin;

import dev.basefinder.freecam.Freecam;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    // The crosshair target is computed from the real player, not the camera, so interacting while
    // detached would act on the wrong place. Block all of it. (require = 0: the packet guard in
    // ClientPacketListenerMixin is the real safety net, so a renamed method here is not fatal.)

    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void basefinder$noAttack(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.isActive()) cir.setReturnValue(false);
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void basefinder$noUse(CallbackInfo ci) {
        if (Freecam.isActive()) ci.cancel();
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true, require = 0)
    private void basefinder$noBreak(boolean down, CallbackInfo ci) {
        if (Freecam.isActive()) ci.cancel();
    }
}
