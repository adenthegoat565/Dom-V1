package com.domclient.client.module.client;

import com.domclient.client.config.ConfigManager;
import com.domclient.client.gui.ClickGuiScreen;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;
import com.domclient.client.module.setting.ActionSetting;
import com.domclient.client.module.setting.BooleanSetting;
import com.domclient.client.module.setting.ModeSetting;
import com.domclient.client.module.setting.NumberSetting;

/** Look and feel of the click GUI itself. Open it with Right Shift (change it in Controls). */
public class ClickGuiModule extends Module {
	private final ModeSetting accent = add(new ModeSetting("Accent Color", "Main colour of the GUI and HUD.", "Purple",
		"Purple", "Blue", "Cyan", "Green", "Yellow", "Orange", "Red", "Pink", "White", "Rainbow"));
	private final NumberSetting rainbowSpeed = add(new NumberSetting("Rainbow Speed", "How fast the rainbow moves.", 1.0, 0.2, 5.0, 0.1, "x"))
		.visibleWhen(() -> isRainbow());
	private final BooleanSetting blur = add(new BooleanSetting("Blur Background", "Blur the game behind the GUI.", true));
	private final NumberSetting darken = add(new NumberSetting("Darken Background", "How dark the background gets.", 35, 0, 90, 5, "%"));
	private final BooleanSetting descriptions = add(new BooleanSetting("Descriptions", "Show what a module does when you hover it.", true));
	private final BooleanSetting sounds = add(new BooleanSetting("Click Sounds", "Play a click when you press things.", true));
	private final BooleanSetting pause = add(new BooleanSetting("Pause Singleplayer", "Pause your world while the GUI is open.", false));

	public ClickGuiModule() {
		super("Click GUI", "Colours and behaviour of this menu. Open it with Right Shift.", Category.CLIENT, Kind.SETTINGS);
		add(new ActionSetting("Reset Panels", "Move every panel back to the top of the screen.", () -> {
			ConfigManager.resetPanels();
			if (mc.gui.screen() instanceof ClickGuiScreen screen) {
				screen.resetLayout();
			}
		}));
	}

	public boolean isRainbow() {
		return accent.is("Rainbow");
	}

	public float rainbowSpeed() {
		return rainbowSpeed.getFloat();
	}

	public int accentColor() {
		return switch (accent.get()) {
			case "Blue" -> 0xFF3B82F6;
			case "Cyan" -> 0xFF06B6D4;
			case "Green" -> 0xFF22C55E;
			case "Yellow" -> 0xFFEAB308;
			case "Orange" -> 0xFFF97316;
			case "Red" -> 0xFFEF4444;
			case "Pink" -> 0xFFEC4899;
			case "White" -> 0xFFE4E4E7;
			default -> 0xFF8B5CF6;
		};
	}

	public boolean blur() {
		return blur.isOn();
	}

	public float darkness() {
		return darken.getFloat() / 100f;
	}

	public boolean descriptions() {
		return descriptions.isOn();
	}

	public boolean sounds() {
		return sounds.isOn();
	}

	public boolean pauseGame() {
		return pause.isOn();
	}
}
