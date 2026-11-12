package com.domclient.client.util;

public final class ColorUtil {
	private ColorUtil() {
	}

	public static int withAlpha(int argb, int alpha) {
		return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
	}

	public static int darker(int argb, float factor) {
		int a = argb >>> 24;
		int r = (int) (((argb >> 16) & 0xFF) * factor);
		int g = (int) (((argb >> 8) & 0xFF) * factor);
		int b = (int) ((argb & 0xFF) * factor);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	public static int lerp(int from, int to, float t) {
		t = Math.max(0, Math.min(1, t));
		int a = (int) ((from >>> 24) + ((to >>> 24) - (from >>> 24)) * t);
		int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * t);
		int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * t);
		int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
		return (a << 24) | (r << 16) | (g << 8) | b;
	}

	/** HSV to opaque ARGB. Hue wraps around, saturation and value are 0..1. */
	public static int hsv(float hue, float saturation, float value) {
		hue = hue - (float) Math.floor(hue);
		float h = hue * 6f;
		int sector = (int) Math.floor(h);
		float f = h - sector;
		float p = value * (1 - saturation);
		float q = value * (1 - saturation * f);
		float t = value * (1 - saturation * (1 - f));
		float r, g, b;
		switch (sector) {
			case 0 -> { r = value; g = t; b = p; }
			case 1 -> { r = q; g = value; b = p; }
			case 2 -> { r = p; g = value; b = t; }
			case 3 -> { r = p; g = q; b = value; }
			case 4 -> { r = t; g = p; b = value; }
			default -> { r = value; g = p; b = q; }
		}
		return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
	}
}
