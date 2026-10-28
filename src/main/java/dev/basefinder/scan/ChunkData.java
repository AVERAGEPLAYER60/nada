package dev.basefinder.scan;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

/** Result of scanning one chunk. */
public final class ChunkData {
    public record EspTarget(BlockPos pos, Signature type) {}

    public final ChunkPos pos;
    public final int[] counts = new int[Signature.values().length];
    public final List<EspTarget> targets = new ArrayList<>();
    public double score;
    public boolean flagged;
    public int minY = Integer.MAX_VALUE;
    public int maxY = Integer.MIN_VALUE;

    public ChunkData(ChunkPos pos) {
        this.pos = pos;
    }

    public void includeY(int lo, int hi) {
        if (lo < minY) minY = lo;
        if (hi > maxY) maxY = hi;
    }

    public boolean hasBounds() {
        return minY <= maxY;
    }

    public boolean isInteresting() {
        return flagged || !targets.isEmpty();
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        for (Signature s : Signature.values()) {
            int c = counts[s.ordinal()];
            if (c <= 0) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s.name().toLowerCase()).append(" x").append(c);
        }
        return sb.toString();
    }
}
