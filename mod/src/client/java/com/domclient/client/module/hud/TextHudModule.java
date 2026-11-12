package com.domclient.client.module.hud;

import com.domclient.client.gui.Theme;
import com.domclient.client.module.Category;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.client.HudSettingsModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import java.util.ArrayList;
import java.util.List;

/** HUD element made of "Label: value" lines. */
public abstract class TextHudModule extends HudModule {
	public record Line(String label, String value, int valueColor) {
		public Line(String label, String value) {
			this(label, value, Theme.TEXT);
		}
	}

	private final List<Line> lines = new ArrayList<>();

	protected TextHudModule(String name, String description, Corner corner, boolean enabledByDefault) {
		super(name, description, corner, enabledByDefault);
	}

	protected TextHudModule(String name, String description, Category category, Corner corner, boolean enabledByDefault) {
		super(name, description, category, corner, enabledByDefault);
	}

	/** Fill {@code out} with the lines to show. Leave it empty to hide the element this frame. */
	protected abstract void collectLines(List<Line> out);

	protected void collectPreviewLines(List<Line> out) {
		collectLines(out);
	}

	@Override
	public boolean measure(Font font) {
		lines.clear();
		collectLines(lines);
		return layout(font);
	}

	@Override
	public boolean measurePreview(Font font) {
		lines.clear();
		collectPreviewLines(lines);
		return layout(font);
	}

	private boolean layout(Font font) {
		if (lines.isEmpty()) {
			width = height = 0;
			return false;
		}
		HudSettingsModule settings = ModuleManager.get(HudSettingsModule.class);
		int pad = settings.backgrounds() ? 2 : 0;
		int w = 0;
		for (Line line : lines) {
			w = Math.max(w, font.width(text(line)));
		}
		width = w + pad * 2;
		height = lines.size() * (font.lineHeight + 1) - 1 + pad * 2;
		return true;
	}

	private static String text(Line line) {
		return line.label().isEmpty() ? line.value() : line.label() + ": " + line.value();
	}

	@Override
	public void draw(GuiGraphicsExtractor g, Font font, int x, int y) {
		drawLines(g, font, x, y);
	}

	@Override
	public void drawPreview(GuiGraphicsExtractor g, Font font, int x, int y) {
		drawLines(g, font, x, y);
	}

	private void drawLines(GuiGraphicsExtractor g, Font font, int x, int y) {
		HudSettingsModule settings = ModuleManager.get(HudSettingsModule.class);
		boolean shadow = settings.textShadow();
		int pad = settings.backgrounds() ? 2 : 0;
		if (settings.backgrounds()) {
			g.fill(x, y, x + width, y + height, 0x90000000);
		}
		boolean rightAligned = isRightSide(g);
		int ly = y + pad;
		int i = 0;
		for (Line line : lines) {
			int lineWidth = font.width(text(line));
			int lx = rightAligned ? x + width - pad - lineWidth : x + pad;
			if (line.label().isEmpty()) {
				g.text(font, line.value(), lx, ly, line.valueColor(), shadow);
			} else {
				String label = line.label() + ": ";
				g.text(font, label, lx, ly, settings.labelColor(i * 0.08f), shadow);
				g.text(font, line.value(), lx + font.width(label), ly, line.valueColor(), shadow);
			}
			ly += font.lineHeight + 1;
			i++;
		}
	}

	private boolean isRightSide(GuiGraphicsExtractor g) {
		return getDrawnX() + width / 2 > g.guiWidth() / 2;
	}
}
