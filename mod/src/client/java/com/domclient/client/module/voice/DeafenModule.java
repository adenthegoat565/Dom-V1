package com.domclient.client.module.voice;

import com.domclient.client.integration.VoiceChatBridge;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;

/** Mirrors Simple Voice Chat's "Disable Voice Chat": you can't hear anyone and nobody hears you. */
public class DeafenModule extends Module {
	public DeafenModule() {
		super("Deafen", "Turn voice chat off completely (no sound in or out).", Category.VOICE);
		addBindSetting();
	}

	@Override
	public boolean isEnabled() {
		return VoiceChatBridge.isInstalled() && VoiceChatBridge.isDeafened();
	}

	@Override
	public void setEnabled(boolean enabled) {
		VoiceChatBridge.setDeafened(enabled);
	}

	@Override
	public void loadEnabled(boolean enabled) {
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
