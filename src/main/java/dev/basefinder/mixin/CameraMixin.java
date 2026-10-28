package dev.basefinder.mixin;

import dev.basefinder.freecam.Freecam;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    /**
     * Runs after vanilla has placed the camera, so whatever we set here wins: the camera ignores
     * collision and can sit inside blocks. (Handler args are omitted on purpose so a changed
     * parameter list in a future patch doesn't break the signature.)
     */
    @Inject(method = "update", at = @At("TAIL"))
    private void basefinder$freecamUpdate(CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        float pt = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        this.setPosition(Freecam.lerpX(pt), Freecam.lerpY(pt), Freecam.lerpZ(pt));
        this.setRotation(Freecam.yaw(), Freecam.pitch());
    }

    /** Makes the renderer draw your own body where it actually is. */
    @Inject(method = "isDetached", at = @At("HEAD"), cancellable = true, require = 0)
    private void basefinder$showBody(CallbackInfoReturnable<Boolean> cir) {
        if (Freecam.isActive()) cir.setReturnValue(true);
    }
}
