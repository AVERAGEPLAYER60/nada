package dev.basefinder.mixin;

import dev.basefinder.freecam.Freecam;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin {
    /**
     * Safety net. Nothing in this mod ever moves the player entity, so movement packets already carry
     * the real position (and must keep flowing, or teleport confirmations would break and you'd
     * rubberband). Interaction packets are dropped while detached.
     */
    @Inject(method = "send", at = @At("HEAD"), cancellable = true)
    private void basefinder$guard(Packet<?> packet, CallbackInfo ci) {
        if (!Freecam.isActive()) return;
        if (packet instanceof ServerboundUseItemOnPacket
                || packet instanceof ServerboundInteractPacket
                || packet instanceof ServerboundUseItemPacket
                || packet instanceof ServerboundPlayerActionPacket) {
            ci.cancel();
        }
    }
}
