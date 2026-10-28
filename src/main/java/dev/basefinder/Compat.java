package dev.basefinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Every call whose name or location has changed between recent Minecraft versions is kept in this one
 * file, so a future version bump (or a compile error) only needs fixing here.
 */
public final class Compat {
    private Compat() {}

    /** 26.2+ moved screen management from Minecraft onto Gui. */
    public static void openScreen(Minecraft mc, Screen screen) {
        mc.gui.setScreen(screen);
    }

    public static boolean noScreenOpen(Minecraft mc) {
        return mc.gui.screen() == null;
    }

    /** Action-bar style message above the hotbar (26.2+: lives on the Hud). */
    public static void overlay(Minecraft mc, String text) {
        mc.gui.hud.setOverlayMessage(Component.literal(text), false);
    }

    public static void chat(Minecraft mc, String text) {
        if (mc.player != null) mc.player.sendSystemMessage(Component.literal(text));
    }
}
