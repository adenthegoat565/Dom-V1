package com.domclient.client.module.hud;

import net.minecraft.client.multiplayer.PlayerInfo;

import java.util.List;

public class PingModule extends TextHudModule {
	public PingModule() {
		super("Ping", "Shows your connection latency to the server.", Corner.TOP_LEFT, false);
		addBindSetting();
	}

	@Override
	protected void collectLines(List<Line> out) {
		if (mc.player == null || mc.getConnection() == null) {
			return;
		}
		PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
		int ping = info == null ? 0 : info.getLatency();
		int color = ping < 80 ? 0xFF4ADE80 : ping < 160 ? 0xFFFACC15 : 0xFFF87171;
		out.add(new Line("Ping", ping + " ms", color));
	}
}
