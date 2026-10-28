package dev.basefinder.mixin;

import dev.basefinder.freecam.Freecam;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    /** Mouse look rotates the freecam instead of the player, so no look packets change. */
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void basefinder$redirectLook(double yRot, double xRot, CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        Object self = this;
        if (self == mc.player || self == mc.getCameraEntity()) {
            Freecam.look(yRot, xRot);
            ci.cancel();
        }
    }
}
