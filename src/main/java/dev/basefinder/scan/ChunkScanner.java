package dev.basefinder.scan;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import dev.basefinder.Config;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * Time-budgeted chunk scanner.
 *
 * Why not a worker thread: client chunk storage (palettes, block-entity maps) is mutated on the main
 * thread with no locking, so reading it from another thread can throw or return garbage. Instead the
 * work is spread over ticks with a hard per-tick time budget (Config.scanBudgetMs). Block entities
 * are read straight from the chunk's map (no block iteration), and the only block-state scan
 * (obsidian) reads palette counts per section.
 */
public final class ChunkScanner {
    private static final Map<Long, ChunkData> DATA = new HashMap<>();
    private static final Map<Long, ChunkPos> LOADED = new HashMap<>();
    private static final ArrayDeque<Long> QUEUE = new ArrayDeque<>();
    private static final Set<Long> QUEUED = new HashSet<>();

    private static ClientLevel currentLevel;
    private static int ticksSinceFullScan;
    private static int ticksSincePrune;

    private ChunkScanner() {}

    public static void init() {
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> {
            if (level != currentLevel) { // dimension change / new connection
                reset();
                currentLevel = level;
            }
            ChunkPos cp = chunk.getPos();
            long key = keyOf(cp);
            LOADED.put(key, cp);
            enqueue(key);
        });

        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> {
            if (level != currentLevel) return;
            long key = keyOf(chunk.getPos());
            LOADED.remove(key);
            DATA.remove(key);
        });

        ClientTickEvents.END_CLIENT_TICK.register(ChunkScanner::tick);
    }

    public static void reset() {
        DATA.clear();
        LOADED.clear();
        QUEUE.clear();
        QUEUED.clear();
        currentLevel = null;
        ticksSinceFullScan = 0;
    }

    public static void requestFullScan() {
        ticksSinceFullScan = Config.rescanIntervalTicks;
    }

    public static Collection<ChunkData> all() {
        return DATA.values();
    }

    /** Map key from the chunk coordinates (independent of ChunkPos' packing helpers, which keep changing). */
    private static long keyOf(ChunkPos cp) {
        long cx = cp.getMinBlockX() >> 4;
        long cz = cp.getMinBlockZ() >> 4;
        return (cx << 32) | (cz & 0xFFFFFFFFL);
    }

    private static void enqueue(long key) {
        if (QUEUED.add(key)) QUEUE.add(key);
    }

    private static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || level != currentLevel) return;
        if (!Config.chunkFlagsEnabled && !Config.espEnabled) return;

        if (++ticksSinceFullScan >= Config.rescanIntervalTicks) {
            ticksSinceFullScan = 0;
            for (Long key : LOADED.keySet()) enqueue(key);
        }

        long deadline = System.nanoTime() + (long) (Config.scanBudgetMs * 1_000_000.0);
        do {
            Long key = QUEUE.poll();
            if (key == null) break;
            QUEUED.remove(key);

            ChunkPos cp = LOADED.get(key);
            if (cp == null) continue;
            LevelChunk chunk = level.getChunkSource().getChunkNow(cp.getMinBlockX() >> 4, cp.getMinBlockZ() >> 4);
            if (chunk == null) continue;

            ChunkData data = scan(chunk);
            if (data.isInteresting()) DATA.put(key, data);
            else DATA.remove(key);
        } while (System.nanoTime() < deadline);

        if (++ticksSincePrune >= 40) { // drop ESP targets whose block entity vanished
            ticksSincePrune = 0;
            for (ChunkData d : DATA.values()) {
                d.targets.removeIf(t -> Signature.classify(level.getBlockEntity(t.pos())) != t.type());
            }
        }
    }

    private static ChunkData scan(LevelChunk chunk) {
        ChunkData d = new ChunkData(chunk.getPos());

        // 1) Block entities: chests, barrels, hoppers, shulkers, spawners, beacons, furnaces, brewing stands.
        //    Also bucket them into 8x8x8 cells so a tight cluster stands out from a sparse spread.
        Map<Integer, Double> cells = new HashMap<>();
        double bestCell = 0.0;

        for (BlockEntity be : chunk.getBlockEntities().values()) {
            Signature sig = Signature.classify(be);
            if (sig == null) continue;

            BlockPos p = be.getBlockPos();
            d.counts[sig.ordinal()]++;
            d.score += sig.weight;
            d.includeY(p.getY(), p.getY());

            int cell = ((p.getY() >> 3) << 2) | (((p.getX() & 15) >> 3) << 1) | ((p.getZ() & 15) >> 3);
            bestCell = Math.max(bestCell, cells.merge(cell, sig.weight, Double::sum));

            if (sig.esp) d.targets.add(new ChunkData.EspTarget(p, sig));
        }

        // 2) Obsidian (not a block entity): per-section palette counts, empty sections skipped.
        LevelChunkSection[] sections = chunk.getSections();
        int bottom = chunk.getMinSectionY();
        int obsidian = 0;
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection section = sections[i];
            if (section == null || section.hasOnlyAir()) continue;

            int[] n = {0};
            section.getStates().count((state, count) -> {
                if (state.is(Blocks.OBSIDIAN)) n[0] += count;
            });
            if (n[0] > 0) {
                obsidian += n[0];
                int baseY = (bottom + i) << 4;
                d.includeY(baseY, baseY + 15);
            }
        }
        if (obsidian > 0) {
            d.counts[Signature.OBSIDIAN.ordinal()] = obsidian;
            d.score += Math.min(obsidian * Signature.OBSIDIAN.weight, Config.obsidianCap);
        }

        d.flagged = d.score >= Config.flagTotalScore || bestCell >= Config.flagClusterScore;
        return d;
    }
}
