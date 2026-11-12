package com.domclient.client.module.hud;

import java.util.List;
import java.util.Locale;

public class SpeedModule extends TextHudModule {
	public SpeedModule() {
		super("Speed", "Shows how fast you're moving in blocks per second.", Corner.TOP_LEFT, false);
		addBindSetting();
	}

	@Override
	protected void collectLines(List<Line> out) {
		if (mc.player == null) {
			return;
		}
		double dx = mc.player.getX() - mc.player.xo;
		double dz = mc.player.getZ() - mc.player.zo;
		double speed = Math.sqrt(dx * dx + dz * dz) * 20.0;
		out.add(new Line("Speed", String.format(Locale.ROOT, "%.1f m/s", speed)));
	}
}
