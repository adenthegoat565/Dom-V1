package com.domclient.client.gui;

import com.domclient.client.module.Module;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.hud.HudModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** Places and draws every enabled HUD module. Registered with Fabric's HUD element registry. */
public final class HudRenderer {
	private static final int EDGE = 3;

	private HudRenderer() {
	}

	public static void render(GuiGraphicsExtractor g, DeltaTracker deltaTracker) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden() || mc.getDebugOverlay().showDebugScreen()) {
			return;
		}
		if (mc.gui.screen() instanceof HudEditorScreen) {
			return;
		}
		layoutAndDraw(g, mc.font, false);
	}

	/** Lays out the HUD and draws it. In preview mode every module that's on is drawn even if it has nothing to show. */
	public static void layoutAndDraw(GuiGraphicsExtractor g, Font font, boolean preview) {
		int sw = g.guiWidth();
		int sh = g.guiHeight();
		int topLeft = EDGE;
		int topRight = EDGE;
		int bottomLeft = sh - EDGE;
		// Keep clear of the hotbar area on the bottom right.
		int bottomRight = sh - EDGE;
		for (Module module : ModuleManager.all()) {
			if (!(module instanceof HudModule hud) || !hud.isEnabled() || !hud.isAvailable()) {
				continue;
			}
			boolean visible = preview ? hud.measurePreview(font) : hud.measure(font);
			if (!visible) {
				continue;
			}
			int w = hud.getWidth();
			int h = hud.getHeight();
			int x;
			int y;
			if (hud.hasCustomPosition()) {
				x = Math.round(EDGE + hud.getRelX() * Math.max(0, sw - w - EDGE * 2));
				y = Math.round(EDGE + hud.getRelY() * Math.max(0, sh - h - EDGE * 2));
			} else {
				switch (hud.getCorner()) {
					case TOP_RIGHT -> {
						x = sw - w - EDGE;
						y = topRight;
						topRight += h + 3;
					}
					case BOTTOM_LEFT -> {
						x = EDGE;
						y = bottomLeft - h;
						bottomLeft -= h + 3;
					}
					case BOTTOM_RIGHT -> {
						x = sw - w - EDGE;
						y = bottomRight - h;
						bottomRight -= h + 3;
					}
					default -> {
						x = EDGE;
						y = topLeft;
						topLeft += h + 3;
					}
				}
			}
			hud.setDrawnAt(x, y);
			if (preview) {
				hud.drawPreview(g, font, x, y);
			} else {
				hud.draw(g, font, x, y);
			}
		}
	}
}
