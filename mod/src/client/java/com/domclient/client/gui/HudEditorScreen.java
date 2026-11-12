package com.domclient.client.gui;

import com.domclient.client.config.ConfigManager;
import com.domclient.client.module.Module;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.hud.HudModule;
import com.domclient.client.util.ColorUtil;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/** Drag HUD elements around. Right-click one to snap it back to its default spot. */
public class HudEditorScreen extends Screen {
	private final @Nullable Screen parent;
	private @Nullable HudModule dragging;
	private double offsetX;
	private double offsetY;

	public HudEditorScreen(@Nullable Screen parent) {
		super(Component.literal("Edit HUD Layout"));
		this.parent = parent;
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, width, height, 0x40000000);
		// Light grid so it's easy to line things up.
		for (int x = 0; x < width; x += 20) {
			graphics.fill(x, 0, x + 1, height, 0x14FFFFFF);
		}
		for (int y = 0; y < height; y += 20) {
			graphics.fill(0, y, width, y + 1, 0x14FFFFFF);
		}
		graphics.fill(width / 2, 0, width / 2 + 1, height, 0x30FFFFFF);
		graphics.fill(0, height / 2, width, height / 2 + 1, 0x30FFFFFF);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		HudRenderer.layoutAndDraw(graphics, font, true);
		HudModule hovered = moduleAt(mouseX, mouseY);
		for (Module module : ModuleManager.all()) {
			if (module instanceof HudModule hud && hud.isEnabled() && hud.isAvailable() && hud.getWidth() > 0) {
				boolean active = hud == hovered || hud == dragging;
				int color = active ? Theme.accent() : ColorUtil.withAlpha(Theme.accent(), 0x88);
				graphics.outline(hud.getDrawnX() - 2, hud.getDrawnY() - 2, hud.getWidth() + 4, hud.getHeight() + 4, color);
				if (active) {
					graphics.text(font, hud.getName(), hud.getDrawnX(), Math.max(1, hud.getDrawnY() - 12), Theme.TEXT, true);
				}
			}
		}
		String hint = "Drag elements to move them  •  Right-click resets one  •  Esc to go back";
		graphics.text(font, hint, (width - font.width(hint)) / 2, height - 14, Theme.TEXT_DIM, true);
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	private @Nullable HudModule moduleAt(double mx, double my) {
		HudModule found = null;
		for (Module module : ModuleManager.all()) {
			if (module instanceof HudModule hud && hud.isEnabled() && hud.isAvailable() && hud.getWidth() > 0) {
				if (mx >= hud.getDrawnX() - 2 && mx < hud.getDrawnX() + hud.getWidth() + 2
					&& my >= hud.getDrawnY() - 2 && my < hud.getDrawnY() + hud.getHeight() + 2) {
					found = hud;
				}
			}
		}
		return found;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		HudModule hud = moduleAt(event.x(), event.y());
		if (hud == null) {
			return super.mouseClicked(event, doubleClick);
		}
		if (event.button() == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
			hud.resetPosition();
			return true;
		}
		dragging = hud;
		offsetX = event.x() - hud.getDrawnX();
		offsetY = event.y() - hud.getDrawnY();
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (dragging == null) {
			return super.mouseDragged(event, dx, dy);
		}
		int edge = 3;
		double x = event.x() - offsetX;
		double y = event.y() - offsetY;
		double spanX = Math.max(1, width - dragging.getWidth() - edge * 2);
		double spanY = Math.max(1, height - dragging.getHeight() - edge * 2);
		dragging.setCustomPosition((float) ((x - edge) / spanX), (float) ((y - edge) / spanY));
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (dragging != null) {
			dragging = null;
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		return super.keyPressed(event);
	}

	@Override
	public void removed() {
		ConfigManager.save();
		super.removed();
	}

	@Override
	public void onClose() {
		minecraft.gui.setScreen(parent);
	}
}
