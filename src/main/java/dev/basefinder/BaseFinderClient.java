package dev.basefinder;

import com.mojang.blaze3d.platform.InputConstants;

import dev.basefinder.freecam.Freecam;
import dev.basefinder.gui.BaseFinderScreen;
import dev.basefinder.render.BaseFinderRenderer;
import dev.basefinder.scan.ChunkScanner;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

public class BaseFinderClient implements ClientModInitializer {
    private static KeyMapping freecamKey;
    private static KeyMapping espKey;
    private static KeyMapping chunksKey;
    private static KeyMapping menuKey;

    @Override
    public void onInitializeClient() {
        // 26.3 switched from GLFW to SDL: always use InputConstants, never raw GLFW constants.
        freecamKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.basefinder.freecam", InputConstants.KEY_G, KeyMapping.Category.MISC));
        espKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.basefinder.esp", InputConstants.KEY_H, KeyMapping.Category.MISC));
        chunksKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.basefinder.chunks", InputConstants.KEY_J, KeyMapping.Category.MISC));
        menuKey = KeyMappingHelper.registerKeyMapping(
                new KeyMapping("key.basefinder.menu", InputConstants.KEY_K, KeyMapping.Category.MISC));

        ChunkScanner.init();
        BaseFinderRenderer.init();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (freecamKey.consumeClick()) {
                Freecam.toggle(client);
            }
            while (espKey.consumeClick()) {
                Config.espEnabled = !Config.espEnabled;
                if (Config.espEnabled) ChunkScanner.requestFullScan();
                notifyToggle(client, "Block ESP", Config.espEnabled);
            }
            while (chunksKey.consumeClick()) {
                Config.chunkFlagsEnabled = !Config.chunkFlagsEnabled;
                if (Config.chunkFlagsEnabled) ChunkScanner.requestFullScan();
                notifyToggle(client, "Chunk flags", Config.chunkFlagsEnabled);
            }
            while (menuKey.consumeClick()) {
                if (client.level != null && Compat.noScreenOpen(client)) {
                    Compat.openScreen(client, new BaseFinderScreen());
                }
            }
            Freecam.tick(client);
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            Freecam.reset();
            ChunkScanner.reset();
        });
    }

    private static void notifyToggle(Minecraft client, String name, boolean on) {
        Compat.overlay(client, name + ": " + (on ? "ON" : "OFF"));
    }
}
