package dev.basefinder.scan;

import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;

/**
 * Things that rarely show up (or show up sparsely) in natural world generation.
 * weight = how suspicious one of them is. esp = also highlighted by Block ESP.
 */
public enum Signature {
    CHEST         (1.00, true,  1.00f, 0.60f, 0.10f),
    TRAPPED_CHEST (1.00, true,  1.00f, 0.20f, 0.20f),
    ENDER_CHEST   (2.00, true,  0.65f, 0.25f, 1.00f),
    SHULKER_BOX   (3.00, true,  1.00f, 0.30f, 0.85f),
    SPAWNER       (2.00, true,  0.20f, 0.90f, 1.00f),
    BARREL        (1.00, false, 0.60f, 0.40f, 0.20f),
    HOPPER        (3.00, false, 0.70f, 0.70f, 0.70f),
    FURNACE       (0.50, false, 0.90f, 0.50f, 0.20f),
    BREWING_STAND (2.00, false, 0.90f, 0.80f, 0.30f),
    BEACON        (6.00, false, 1.00f, 1.00f, 1.00f),
    OBSIDIAN      (0.05, false, 0.30f, 0.10f, 0.50f); // not a block entity; counted from palettes

    public final double weight;
    public final boolean esp;
    public final float r, g, b;

    Signature(double weight, boolean esp, float r, float g, float b) {
        this.weight = weight;
        this.esp = esp;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /** Maps a block entity to a signature, or null if it isn't interesting. */
    public static Signature classify(BlockEntity be) {
        if (be == null) return null;
        if (be instanceof TrappedChestBlockEntity) return TRAPPED_CHEST; // before ChestBlockEntity (subclass)
        if (be instanceof ChestBlockEntity) return CHEST;
        if (be instanceof EnderChestBlockEntity) return ENDER_CHEST;
        if (be instanceof ShulkerBoxBlockEntity) return SHULKER_BOX;
        if (be instanceof SpawnerBlockEntity) return SPAWNER;
        if (be instanceof BarrelBlockEntity) return BARREL;
        if (be instanceof HopperBlockEntity) return HOPPER;
        if (be instanceof AbstractFurnaceBlockEntity) return FURNACE;
        if (be instanceof BrewingStandBlockEntity) return BREWING_STAND;
        if (be instanceof BeaconBlockEntity) return BEACON;
        return null;
    }
}
