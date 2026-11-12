package com.domclient.client.module.hud;

import com.domclient.client.module.setting.ModeSetting;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class ClockModule extends TextHudModule {
	private static final DateTimeFormatter H24 = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
	private static final DateTimeFormatter H12 = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);
	private final ModeSetting format = add(new ModeSetting("Format", "12 or 24 hour clock.", "12h", "12h", "24h"));

	public ClockModule() {
		super("Clock", "Shows the real-world time.", Corner.TOP_LEFT, false);
		addBindSetting();
	}

	@Override
	protected void collectLines(List<Line> out) {
		LocalTime now = LocalTime.now();
		out.add(new Line("Time", (format.is("24h") ? H24 : H12).format(now)));
	}
}
