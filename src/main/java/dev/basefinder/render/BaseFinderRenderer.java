package dev.basefinder.render;

import dev.basefinder.Config;
import dev.basefinder.freecam.Freecam;
import dev.basefinder.scan.ChunkData;
import dev.basefinder.scan.ChunkScanner;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelExtractionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Draws ESP boxes and flagged-chunk boxes with vanilla gizmos marked "always on top", which is how
 * Minecraft itself draws debug shapes through walls. 26.x replaced immediate-mode drawing with a
 * submit/extract renderer, so going through Gizmos avoids writing any GPU pipeline code.
 */
public final class BaseFinderRenderer {
    private static final Logger LOG = LoggerFactory.getLogger("basefinder");
    private static boolean failedOnce;

    private BaseFinderRenderer() {}

    public static void init() {
        LevelExtractionEvents.END_EXTRACTION.register(context -> {
            try {
                draw();
            } catch (RuntimeException e) {
                if (!failedOnce) { // log once, don't spam every frame
                    failedOnce = true;
                    LOG.error("Base Finder rendering failed (gizmo API changed?)", e);
                }
            }
        });
    }

    private static int argb(float r, float g, float b, int a) {
        return (a << 24) | ((int) (r * 255f) << 16) | ((int) (g * 255f) << 8) | (int) (b * 255f);
    }

    private static void draw() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (!Config.espEnabled && !Config.chunkFlagsEnabled) return;

        // Distance is measured from the camera: the freecam position when detached, else your eyes.
        Vec3 origin = Freecam.isActive()
                ? new Vec3(Freecam.x(), Freecam.y(), Freecam.z())
                : mc.player.getEyePosition();
        double rangeSq = (double) Config.espRangeBlocks * Config.espRangeBlocks;
        int espCount = 0;

        for (ChunkData d : ChunkScanner.all()) {
            if (Config.espEnabled) {
                for (ChunkData.EspTarget t : d.targets) {
                    if (espCount >= Config.espMaxBoxes) break;
                    BlockPos p = t.pos();
                    if (origin.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) > rangeSq) continue;

                    AABB box = new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 1, p.getZ() + 1).inflate(0.002);
                    int stroke = argb(t.type().r, t.type().g, t.type().b, 255);
                    int fill = argb(t.type().r, t.type().g, t.type().b, 46);
                    Gizmos.cuboid(box, GizmoStyle.strokeAndFill(stroke, 2.0f, fill)).setAlwaysOnTop();
                    espCount++;
                }
            }

            if (Config.chunkFlagsEnabled && d.flagged && d.hasBounds()) {
                ChunkPos cp = d.pos;
                boolean strong = d.score >= Config.flagTotalScore * 2.0;
                double y1 = d.minY - 1;
                double y2 = Math.max(d.maxY + 2, d.minY + 8);
                AABB box = new AABB(cp.getMinBlockX(), y1, cp.getMinBlockZ(),
                        cp.getMaxBlockX() + 1, y2, cp.getMaxBlockZ() + 1);
                int color = strong ? argb(1.0f, 0.15f, 0.10f, 230) : argb(1.0f, 0.90f, 0.10f, 230);
                Gizmos.cuboid(box, GizmoStyle.stroke(color, 3.0f)).setAlwaysOnTop();
            }
        }
    }
}
