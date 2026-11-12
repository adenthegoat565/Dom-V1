package com.domclient.client.module.voice;

import com.domclient.client.integration.VoiceChatBridge;
import com.domclient.client.module.Category;
import com.domclient.client.module.hud.TextHudModule;

import java.util.List;

public class VoiceStatusModule extends TextHudModule {
	public VoiceStatusModule() {
		super("Voice HUD", "Shows whether voice chat is connected, muted or deafened, and your group.", Category.VOICE, Corner.TOP_LEFT, true);
		addBindSetting();
	}

	@Override
	public boolean isAvailable() {
		return VoiceChatBridge.isInstalled();
	}

	@Override
	public String getUnavailableReason() {
		return "Simple Voice Chat is not installed.";
	}

	@Override
	protected void collectLines(List<Line> out) {
		if (!VoiceChatBridge.isInstalled() || mc.player == null) {
			return;
		}
		if (!VoiceChatBridge.isConnected()) {
			out.add(new Line("Voice", "Not connected", 0xFF9CA3AF));
			return;
		}
		if (VoiceChatBridge.isDeafened()) {
			out.add(new Line("Voice", "Deafened", 0xFFF87171));
		} else if (VoiceChatBridge.isMuted()) {
			out.add(new Line("Voice", "Muted", 0xFFFB923C));
		} else if (VoiceChatBridge.isTalking()) {
			out.add(new Line("Voice", VoiceChatBridge.isWhispering() ? "Whispering" : "Talking", 0xFF4ADE80));
		} else {
			out.add(new Line("Voice", "Connected", 0xFFFFFFFF));
		}
		String group = VoiceChatBridge.groupName();
		if (group != null && !group.isEmpty()) {
			out.add(new Line("Group", group));
		}
	}

	@Override
	protected void collectPreviewLines(List<Line> out) {
		collectLines(out);
		if (out.isEmpty()) {
			out.add(new Line("Voice", "Connected"));
		}
	}
}
