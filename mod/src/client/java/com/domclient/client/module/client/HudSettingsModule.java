package com.domclient.client.module.client;

import com.domclient.client.gui.Theme;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;
import com.domclient.client.module.setting.BooleanSetting;
import com.domclient.client.module.setting.ModeSetting;

public class HudSettingsModule extends Module {
	private final BooleanSetting shadow = add(new BooleanSetting("Text Shadow", "Draw HUD text with a shadow.", true));
	private final BooleanSetting backgrounds = add(new BooleanSetting("Backgrounds", "Dark box behind HUD text.", false));
	private final ModeSetting labels = add(new ModeSetting("Label Color", "Colour of labels like \"FPS:\".", "Accent", "Accent", "White", "Gray"));

	public HudSettingsModule() {
		super("HUD Settings", "Text shadow, backgrounds and label colour for HUD elements.", Category.HUD, Kind.SETTINGS);
	}

	public boolean textShadow() {
		return shadow.isOn();
	}

	public boolean backgrounds() {
		return backgrounds.isOn();
	}

	public int labelColor(float offset) {
		return switch (labels.get()) {
			case "White" -> Theme.TEXT;
			case "Gray" -> Theme.TEXT_DIM;
			default -> Theme.accent(offset);
		};
	}
}
