package com.domclient.client.module.voice;

import com.domclient.client.integration.VoiceChatBridge;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;

/** A button that opens one of Simple Voice Chat's own screens. */
public class VoiceScreenModule extends Module {
	private final String keyName;

	public VoiceScreenModule(String name, String description, String keyName) {
		super(name, description, Category.VOICE, Kind.ACTION);
		this.keyName = keyName;
		addBindSetting();
	}

	@Override
	protected void onAction() {
		// Minecraft handles key presses only when no screen is open, so close ours first;
		// Simple Voice Chat then opens its screen on the next tick.
		if (VoiceChatBridge.pressKey(keyName) && mc.gui.screen() != null) {
			mc.gui.setScreen(null);
		}
	}

	@Override
	public boolean isAvailable() {
		return VoiceChatBridge.isInstalled();
	}

	@Override
	public String getUnavailableReason() {
		return "Simple Voice Chat is not installed.";
	}
}
