package com.domclient.client.module.hud;

import java.util.List;

public class FpsModule extends TextHudModule {
	public FpsModule() {
		super("FPS", "Shows your frames per second.", Corner.TOP_LEFT, true);
		addBindSetting();
	}

	@Override
	protected void collectLines(List<Line> out) {
		out.add(new Line("FPS", Integer.toString(mc.getFps())));
	}
}
