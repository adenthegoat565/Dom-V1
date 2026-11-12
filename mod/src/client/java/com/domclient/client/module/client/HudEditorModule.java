package com.domclient.client.module.client;

import com.domclient.client.gui.HudEditorScreen;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;

public class HudEditorModule extends Module {
	public HudEditorModule() {
		super("Edit HUD Layout", "Drag HUD elements wherever you want them.", Category.HUD, Kind.ACTION);
	}

	@Override
	protected void onAction() {
		if (mc.level != null) {
			mc.gui.setScreen(new HudEditorScreen(mc.gui.screen()));
		}
	}

	@Override
	public boolean isAvailable() {
		return mc.level != null;
	}

	@Override
	public String getUnavailableReason() {
		return "Join a world first.";
	}
}
