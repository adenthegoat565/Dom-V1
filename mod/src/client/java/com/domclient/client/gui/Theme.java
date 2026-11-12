package com.domclient.client.gui;

import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.client.ClickGuiModule;
import com.domclient.client.util.ColorUtil;
import net.minecraft.util.Util;

/** Colours shared by the click GUI and the HUD. */
public final class Theme {
	public static final int PANEL_BG = 0xF0141419;
	public static final int SETTINGS_BG = 0xF00D0D11;
	public static final int ROW_HOVER = 0x22FFFFFF;
	public static final int TEXT = 0xFFFFFFFF;
	public static final int TEXT_DIM = 0xFFA1A1AA;
	public static final int TEXT_DISABLED = 0xFF5B5B66;
	public static final int OUTLINE = 0xFF26262E;

	private Theme() {
	}

	/** The accent colour. With Rainbow on, {@code offset} shifts the hue so rows form a gradient. */
	public static int accent(float offset) {
		ClickGuiModule gui = ModuleManager.get(ClickGuiModule.class);
		if (gui != null && gui.isRainbow()) {
			float speed = gui.rainbowSpeed();
			float hue = (Util.getMillis() % 100_000L) / 1000f * 0.1f * speed + offset;
			return ColorUtil.hsv(hue, 0.65f, 1f);
		}
		return gui != null ? gui.accentColor() : 0xFF8B5CF6;
	}

	public static int accent() {
		return accent(0);
	}
}
