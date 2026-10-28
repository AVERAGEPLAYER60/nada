package dev.basefinder.freecam;

import dev.basefinder.Compat;
import dev.basefinder.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * Detached camera.
 *
 * Design: the real player entity is never moved or rotated. Only the Camera is redirected to the
 * state held here (CameraMixin), the player's own movement input is zeroed (KeyboardInputMixin),
 * mouse-look is diverted into this class (EntityMixin), and attack/use/break are blocked
 * (MinecraftMixin). Because the entity's position and rotation never change, the vanilla movement
 * packets the client keeps sending describe your real, stationary body, and the camera position is
 * never written into any packet. ClientPacketListenerMixin additionally drops interaction-type
 * packets while active as a safety net.
 */
public final class Freecam {
    private static boolean active;
    private static double x, y, z, prevX, prevY, prevZ;
    private static float yaw, pitch;

    private Freecam() {}

    public static boolean isActive() { return active; }

    public static double x() { return x; }
    public static double y() { return y; }
    public static double z() { return z; }

    public static double lerpX(float pt) { return Mth.lerp((double) pt, prevX, x); }
    public static double lerpY(float pt) { return Mth.lerp((double) pt, prevY, y); }
    public static double lerpZ(float pt) { return Mth.lerp((double) pt, prevZ, z); }
    public static float yaw() { return yaw; }
    public static float pitch() { return pitch; }

    public static void toggle(Minecraft mc) {
        if (active) disable(mc);
        else enable(mc);
    }

    public static void enable(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) return;
        if (mc.gameMode != null) mc.gameMode.stopDestroyBlock();

        Vec3 eye = player.getEyePosition();
        x = prevX = eye.x;
        y = prevY = eye.y;
        z = prevZ = eye.z;
        yaw = player.getYRot();
        pitch = player.getXRot();
        active = true;
        Compat.overlay(mc, "Freecam: ON");
    }

    public static void disable(Minecraft mc) {
        active = false;
        Compat.overlay(mc, "Freecam: OFF");
    }

    /** Silent reset (disconnect etc.). */
    public static void reset() {
        active = false;
    }

    /** Called from EntityMixin with the raw mouse deltas (same 0.15 deg/unit scale as vanilla). */
    public static void look(double dx, double dy) {
        yaw += (float) (dx * 0.15);
        pitch = Mth.clamp(pitch + (float) (dy * 0.15), -90.0f, 90.0f);
    }

    public static void tick(Minecraft mc) {
        if (!active) return;

        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || !player.isAlive()) {
            active = false;
            return;
        }
        if (Config.disableFreecamOnDamage && player.hurtTime > 0) {
            disable(mc);
            return;
        }

        prevX = x;
        prevY = y;
        prevZ = z;

        Options o = mc.options;
        double forward = (o.keyUp.isDown() ? 1 : 0) - (o.keyDown.isDown() ? 1 : 0);
        double strafe = (o.keyRight.isDown() ? 1 : 0) - (o.keyLeft.isDown() ? 1 : 0);
        double vertical = (o.keyJump.isDown() ? 1 : 0) - (o.keyShift.isDown() ? 1 : 0); // Space / Left Shift

        double len = Math.hypot(forward, strafe);
        if (len > 1.0) {
            forward /= len;
            strafe /= len;
        }

        double speed = Config.freecamSpeed * (o.keySprint.isDown() ? Config.freecamSprintMultiplier : 1.0);
        double rad = Math.toRadians(yaw);
        double sin = Math.sin(rad);
        double cos = Math.cos(rad);

        // Minecraft: yaw 0 faces +Z, forward = (-sin, cos), right = (-cos, -sin)
        x += (-sin * forward - cos * strafe) * speed;
        z += (cos * forward - sin * strafe) * speed;
        y += vertical * speed;
    }
}
