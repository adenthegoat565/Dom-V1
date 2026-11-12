package com.domclient.client.module.voice;

import com.domclient.client.integration.VoiceChatBridge;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;

/** Mirrors Simple Voice Chat's "Mute Microphone". Turning this on mutes you. */
public class MuteModule extends Module {
	public MuteModule() {
		super("Mute Mic", "Mute your microphone in Simple Voice Chat.", Category.VOICE);
		addBindSetting();
	}

	@Override
	public boolean isEnabled() {
		return VoiceChatBridge.isInstalled() && VoiceChatBridge.isMuted();
	}

	@Override
	public void setEnabled(boolean enabled) {
		VoiceChatBridge.setMuted(enabled);
	}

	@Override
	public void loadEnabled(boolean enabled) {
		// Simple Voice Chat remembers this itself.
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
