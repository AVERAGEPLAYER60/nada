package dev.basefinder;

/** All tunables in one place. Everything is read/written on the client thread only. */
public final class Config {
    private Config() {}

    // ---- module toggles ----
    public static boolean espEnabled = true;
    public static boolean chunkFlagsEnabled = true;

    // ---- Freecam ----
    /** Blocks per tick (20 ticks/s). */
    public static double freecamSpeed = 1.0;
    public static double freecamSprintMultiplier = 3.0;
    /** Your real body stays where it was; leave freecam if something hits it. */
    public static boolean disableFreecamOnDamage = true;

    // ---- Chunk flagging ----
    /** Max milliseconds of scanning per client tick. Lower = smoother FPS, slower discovery. */
    public static double scanBudgetMs = 1.5;
    /** Every loaded chunk is re-queued this often (catches blocks placed after the chunk loaded). */
    public static int rescanIntervalTicks = 20 * 60;
    /** Flag a chunk when the weighted score of everything in it reaches this. */
    public static double flagTotalScore = 14.0;
    /** ...or when any single 8x8x8 cell reaches this (a dense cluster). */
    public static double flagClusterScore = 9.0;
    /** Obsidian is common in lava pools / portals, so its contribution is capped. */
    public static double obsidianCap = 3.0;

    // ---- ESP ----
    public static int espRangeBlocks = 160;
    public static int espMaxBoxes = 1500;
}
