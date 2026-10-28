package dev.basefinder.gui;

import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import dev.basefinder.Compat;
import dev.basefinder.Config;
import dev.basefinder.freecam.Freecam;
import dev.basefinder.scan.ChunkData;
import dev.basefinder.scan.ChunkScanner;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Simple settings menu: module toggles, a few sliders, list/rescan, done. Opened with K. */
public class BaseFinderScreen extends Screen {
    private static final int W = 200;
    private static final int H = 20;
    private static final int GAP = 22;

    public BaseFinderScreen() {
        super(Component.literal("Base Finder"));
    }

    @Override
    protected void init() {
        int x = this.width / 2 - W / 2;
        int y = Math.max(10, this.height / 2 - 116);

        // Title row (a disabled button keeps this independent of the text-drawing API)
        Button header = Button.builder(Component.literal("Base Finder"), b -> { })
                .bounds(x, y, W, H).build();
        header.active = false;
        this.addRenderableWidget(header);
        y += GAP + 4;

        // Freecam closes the menu so you can fly right away.
        addToggle(x, y, "Freecam", Freecam::isActive, mc -> {
            Freecam.toggle(mc);
            this.onClose();
        });
        y += GAP;
        addToggle(x, y, "Block ESP", () -> Config.espEnabled, mc -> {
            Config.espEnabled = !Config.espEnabled;
            if (Config.espEnabled) ChunkScanner.requestFullScan();
        });
        y += GAP;
        addToggle(x, y, "Chunk Flags", () -> Config.chunkFlagsEnabled, mc -> {
            Config.chunkFlagsEnabled = !Config.chunkFlagsEnabled;
            if (Config.chunkFlagsEnabled) ChunkScanner.requestFullScan();
        });
        y += GAP + 4;

        this.addRenderableWidget(new ValueSlider(x, y, W, H, "Freecam speed", 0.2, 5.0, false,
                Config.freecamSpeed, v -> Config.freecamSpeed = v));
        y += GAP;
        this.addRenderableWidget(new ValueSlider(x, y, W, H, "ESP range", 32, 256, true,
                Config.espRangeBlocks, v -> Config.espRangeBlocks = (int) Math.round(v)));
        y += GAP;
        this.addRenderableWidget(new ValueSlider(x, y, W, H, "Flag threshold", 4, 40, true,
                Config.flagTotalScore, v -> {
                    Config.flagTotalScore = Math.round(v);
                    Config.flagClusterScore = Config.flagTotalScore * (9.0 / 14.0); // keep the same ratio
                }));
        y += GAP + 4;

        this.addRenderableWidget(Button.builder(Component.literal("List flagged chunks (chat)"), b -> listFlagged())
                .bounds(x, y, W, H).build());
        y += GAP;
        this.addRenderableWidget(Button.builder(Component.literal("Rescan chunks"), b -> ChunkScanner.requestFullScan())
                .bounds(x, y, W, H).build());
        y += GAP;
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> this.onClose())
                .bounds(x, y, W, H).build());
    }

    private void addToggle(int x, int y, String name, BooleanSupplier state, Consumer<Minecraft> onPress) {
        Button button = Button.builder(label(name, state.getAsBoolean()), b -> {
            onPress.accept(Minecraft.getInstance());
            b.setMessage(label(name, state.getAsBoolean()));
        }).bounds(x, y, W, H).build();
        this.addRenderableWidget(button);
    }

    private static Component label(String name, boolean on) {
        return Component.literal(name + ": " + (on ? "ON" : "OFF"));
    }

    private static void listFlagged() {
        Minecraft mc = Minecraft.getInstance();
        List<ChunkData> top = ChunkScanner.all().stream()
                .filter(d -> d.flagged)
                .sorted(Comparator.comparingDouble((ChunkData d) -> d.score).reversed())
                .limit(15)
                .toList();
        if (top.isEmpty()) {
            Compat.chat(mc, "[Base Finder] No flagged chunks yet.");
            return;
        }
        for (ChunkData d : top) {
            Compat.chat(mc, String.format("[Base Finder] Chunk (%d, %d)  score %.1f  [%s]",
                    d.pos.getMiddleBlockX(), d.pos.getMiddleBlockZ(), d.score, d.summary()));
        }
    }

    @Override
    public void onClose() {
        ChunkScanner.requestFullScan(); // re-evaluate flags with the new threshold
        super.onClose();
    }

    /** Slider mapping 0..1 onto [min, max]. */
    private static final class ValueSlider extends AbstractSliderButton {
        private final String label;
        private final double min;
        private final double max;
        private final boolean whole;
        private final DoubleConsumer setter;

        ValueSlider(int x, int y, int width, int height, String label, double min, double max,
                    boolean whole, double current, DoubleConsumer setter) {
            super(x, y, width, height, Component.empty(), (current - min) / (max - min));
            this.label = label;
            this.min = min;
            this.max = max;
            this.whole = whole;
            this.setter = setter;
            updateMessage();
        }

        private double actual() {
            return min + (max - min) * this.value;
        }

        @Override
        protected void updateMessage() {
            if (label == null) return; // may be called from the super constructor
            double v = actual();
            setMessage(Component.literal(label + ": " + (whole ? String.valueOf(Math.round(v)) : String.format("%.1f", v))));
        }

        @Override
        protected void applyValue() {
            setter.accept(actual());
        }
    }
}
