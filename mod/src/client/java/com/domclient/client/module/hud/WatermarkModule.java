package com.domclient.client.module.hud;

import com.domclient.client.DomClient;
import com.domclient.client.gui.Theme;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.client.HudSettingsModule;
import com.domclient.client.module.setting.BooleanSetting;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public class WatermarkModule extends HudModule {
	private final BooleanSetting showVersion = add(new BooleanSetting("Show Version", "Show the mod version after the name.", true));

	public WatermarkModule() {
		super("Watermark", "Shows \"Dom Client\" in the corner.", Corner.TOP_LEFT, true);
		addBindSetting();
	}

	private Component title() {
		return Component.literal("Dom Client").withStyle(ChatFormatting.BOLD);
	}

	private String version() {
		return showVersion.isOn() ? " v" + DomClient.VERSION : "";
	}

	@Override
	public boolean measure(Font font) {
		width = font.width(title()) + font.width(version());
		height = font.lineHeight;
		return true;
	}

	@Override
	public void draw(GuiGraphicsExtractor g, Font font, int x, int y) {
		boolean shadow = ModuleManager.get(HudSettingsModule.class).textShadow();
		g.text(font, title(), x, y, Theme.accent(), shadow);
		if (showVersion.isOn()) {
			g.text(font, version(), x + font.width(title()), y, Theme.TEXT_DIM, shadow);
		}
	}
}
