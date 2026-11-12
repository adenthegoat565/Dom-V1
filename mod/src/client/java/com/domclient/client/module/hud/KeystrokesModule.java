package com.domclient.client.module.hud;

import com.domclient.client.gui.Theme;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.client.HudSettingsModule;
import com.domclient.client.module.setting.BooleanSetting;
import com.domclient.client.util.ColorUtil;
import com.domclient.client.util.CpsTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class KeystrokesModule extends HudModule {
	private static final int KEY = 20;
	private static final int GAP = 2;
	private final BooleanSetting mouse = add(new BooleanSetting("Mouse Buttons", "Show LMB / RMB with clicks per second.", true));
	private final BooleanSetting space = add(new BooleanSetting("Space Bar", "Show the jump key.", true));

	public KeystrokesModule() {
		super("Keystrokes", "Shows which movement keys and mouse buttons you're pressing.", Corner.BOTTOM_RIGHT, false);
		addBindSetting();
	}

	@Override
	public boolean measure(Font font) {
		width = KEY * 3 + GAP * 2;
		height = KEY * 2 + GAP;
		if (mouse.isOn()) {
			height += GAP + KEY;
		}
		if (space.isOn()) {
			height += GAP + 10;
		}
		return true;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, Font font, int x, int y) {
		var o = mc.options;
		key(g, font, x + KEY + GAP, y, KEY, KEY, o.keyUp, label(o.keyUp));
		int row2 = y + KEY + GAP;
		key(g, font, x, row2, KEY, KEY, o.keyLeft, label(o.keyLeft));
		key(g, font, x + KEY + GAP, row2, KEY, KEY, o.keyDown, label(o.keyDown));
		key(g, font, x + (KEY + GAP) * 2, row2, KEY, KEY, o.keyRight, label(o.keyRight));
		int next = row2 + KEY + GAP;
		if (mouse.isOn()) {
			int half = (width - GAP) / 2;
			key(g, font, x, next, half, KEY, o.keyAttack, "LMB", CpsTracker.left() + " CPS");
			key(g, font, x + half + GAP, next, width - half - GAP, KEY, o.keyUse, "RMB", CpsTracker.right() + " CPS");
			next += KEY + GAP;
		}
		if (space.isOn()) {
			boolean down = o.keyJump.isDown();
			box(g, x, next, width, 10, down);
			int lineY = next + 4;
			g.fill(x + width / 2 - 10, lineY, x + width / 2 + 10, lineY + 1, down ? 0xFF000000 : 0xFFFFFFFF);
		}
	}

	private static String label(KeyMapping mapping) {
		String name = mapping.getTranslatedKeyMessage().getString();
		return name.length() > 3 ? name.substring(0, 3) : name;
	}

	private void key(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, KeyMapping mapping, String label) {
		key(g, font, x, y, w, h, mapping, label, null);
	}

	private void key(GuiGraphicsExtractor g, Font font, int x, int y, int w, int h, KeyMapping mapping, String label, String sub) {
		boolean down = mapping.isDown();
		box(g, x, y, w, h, down);
		boolean shadow = ModuleManager.get(HudSettingsModule.class).textShadow() && !down;
		int color = down ? 0xFF000000 : 0xFFFFFFFF;
		if (sub == null) {
			g.text(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2, color, shadow);
		} else {
			g.text(font, label, x + (w - font.width(label)) / 2, y + 2, color, shadow);
			g.pose().pushMatrix();
			g.pose().translate(x + w / 2f, y + 12);
			g.pose().scale(0.5f, 0.5f);
			g.text(font, sub, -font.width(sub) / 2, 0, color, false);
			g.pose().popMatrix();
		}
	}

	private static void box(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean down) {
		int fill = down ? 0xE0FFFFFF : 0x80000000;
		g.fill(x, y, x + w, y + h, fill);
		if (down) {
			g.outline(x, y, w, h, ColorUtil.withAlpha(Theme.accent(), 255));
		}
	}
}
