package com.domclient.client.module.player;

import com.domclient.client.module.Category;
import com.domclient.client.module.Module;

/** Keeps the sprint key held for you, so you always sprint when walking forward. */
public class ToggleSprintModule extends Module {
	public ToggleSprintModule() {
		super("Toggle Sprint", "Always sprint when you walk forward, without holding Ctrl.", Category.PLAYER);
		addBindSetting();
	}

	@Override
	public void onTick() {
		if (!isEnabled() || mc.player == null || mc.gui.screen() != null) {
			return;
		}
		if (!mc.options.keySprint.isDown()) {
			// Works with vanilla's hold and toggle sprint modes: in toggle mode this flips it on.
			mc.options.keySprint.setDown(true);
		}
	}

	@Override
	protected void onDisable() {
		if (mc.options.keySprint.isDown()) {
			if (mc.options.toggleSprint().get()) {
				mc.options.keySprint.setDown(true);
			} else {
				mc.options.keySprint.setDown(false);
			}
		}
	}
}
