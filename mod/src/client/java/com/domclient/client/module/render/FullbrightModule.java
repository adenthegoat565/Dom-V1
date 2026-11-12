package com.domclient.client.module.render;

import com.domclient.client.module.Category;
import com.domclient.client.module.Module;

/** Lights up caves and nights the same way Night Vision does. See LightmapRenderStateExtractorMixin. */
public class FullbrightModule extends Module {
	public FullbrightModule() {
		super("Fullbright", "See in the dark, like a permanent Night Vision effect.", Category.RENDER);
		addBindSetting();
	}
}
