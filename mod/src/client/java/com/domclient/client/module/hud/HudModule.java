package com.domclient.client.module.hud;

import com.domclient.client.module.Category;
import com.domclient.client.module.Module;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** A module that draws something on the in-game HUD and can be moved in the HUD editor. */
public abstract class HudModule extends Module {
	public enum Corner {
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}

	private final Corner corner;
	private final boolean enabledByDefault;
	private boolean customPosition;
	private float relX;
	private float relY;
	protected int width;
	protected int height;
	private int drawnX;
	private int drawnY;

	protected HudModule(String name, String description, Corner corner, boolean enabledByDefault) {
		this(name, description, Category.HUD, corner, enabledByDefault);
	}

	protected HudModule(String name, String description, Category category, Corner corner, boolean enabledByDefault) {
		super(name, description, category);
		this.corner = corner;
		this.enabledByDefault = enabledByDefault;
		loadEnabled(enabledByDefault);
	}

	public boolean isEnabledByDefault() {
		return enabledByDefault;
	}

	/** Works out {@link #width} and {@link #height} for this frame. Return false to draw nothing. */
	public abstract boolean measure(Font font);

	public abstract void draw(GuiGraphicsExtractor g, Font font, int x, int y);

	/** Fake content for the HUD editor when there's nothing real to show (for example empty armor slots). */
	public boolean measurePreview(Font font) {
		return measure(font);
	}

	public void drawPreview(GuiGraphicsExtractor g, Font font, int x, int y) {
		draw(g, font, x, y);
	}

	public Corner getCorner() {
		return corner;
	}

	public boolean hasCustomPosition() {
		return customPosition;
	}

	public float getRelX() {
		return relX;
	}

	public float getRelY() {
		return relY;
	}

	public void setCustomPosition(float relX, float relY) {
		this.customPosition = true;
		this.relX = Math.max(0, Math.min(1, relX));
		this.relY = Math.max(0, Math.min(1, relY));
	}

	public void resetPosition() {
		this.customPosition = false;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public int getDrawnX() {
		return drawnX;
	}

	public int getDrawnY() {
		return drawnY;
	}

	public void setDrawnAt(int x, int y) {
		this.drawnX = x;
		this.drawnY = y;
	}

	@Override
	public boolean showInModuleList() {
		return false;
	}
}
