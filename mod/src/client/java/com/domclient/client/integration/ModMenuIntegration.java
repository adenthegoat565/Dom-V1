package com.domclient.client.integration;

import com.domclient.client.gui.ClickGuiScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Lets you open the click GUI from Mod Menu's "Configure" button. */
public class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ClickGuiScreen::new;
	}
}
