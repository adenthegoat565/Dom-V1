package com.domclient.client.module.hud;

import com.domclient.client.util.CpsTracker;

import java.util.List;

public class CpsModule extends TextHudModule {
	public CpsModule() {
		super("CPS", "Shows left and right clicks per second.", Corner.TOP_LEFT, false);
		addBindSetting();
	}

	@Override
	protected void collectLines(List<Line> out) {
		out.add(new Line("CPS", CpsTracker.left() + " | " + CpsTracker.right()));
	}
}
