package com.domclient.client.module.render;

import com.domclient.client.DomClient;
import com.domclient.client.module.Category;
import com.domclient.client.module.Module;
import com.domclient.client.module.setting.BooleanSetting;
import com.domclient.client.module.setting.NumberSetting;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;

/** Hold the zoom key (C by default, change it in Controls) to zoom in like a spyglass. */
public class ZoomModule extends Module {
	private final NumberSetting level = add(new NumberSetting("Zoom Level", "How far you zoom in.", 4.0, 1.5, 10.0, 0.5, "x"));
	private final BooleanSetting smooth = add(new BooleanSetting("Smooth", "Animate zooming in and out.", true));
	private final BooleanSetting scroll = add(new BooleanSetting("Scroll to Adjust", "Scroll the mouse wheel while zooming to change the zoom.", true));
	private final BooleanSetting lowerSensitivity = add(new BooleanSetting("Lower Sensitivity", "Turn slower while zoomed so aiming is easier.", true));
	private final BooleanSetting cinematic = add(new BooleanSetting("Cinematic Camera", "Use smooth cinematic camera while zoomed.", false));

	private boolean zooming;
	private double scrollLevel;
	private float current = 1f;
	private long lastUpdate = Util.getMillis();
	private boolean savedSmoothCamera;

	public ZoomModule() {
		super("Zoom", "Hold the zoom key (C by default) to zoom in.", Category.RENDER);
		addBindSetting();
		loadEnabled(true);
	}

	@Override
	public String getInfo() {
		return level.getDisplayValue();
	}

	@Override
	public void onTick() {
		boolean wants = isEnabled() && mc.gui.screen() == null && DomClient.ZOOM_KEY.isDown();
		if (wants && !zooming) {
			zooming = true;
			scrollLevel = level.get();
			if (cinematic.isOn()) {
				savedSmoothCamera = mc.options.smoothCamera;
				mc.options.smoothCamera = true;
			}
		} else if (!wants && zooming) {
			zooming = false;
			if (cinematic.isOn()) {
				mc.options.smoothCamera = savedSmoothCamera;
			}
		}
	}

	@Override
	protected void onDisable() {
		if (zooming && cinematic.isOn()) {
			mc.options.smoothCamera = savedSmoothCamera;
		}
		zooming = false;
	}

	public boolean isZooming() {
		return zooming;
	}

	/** Divisor applied to the field of view this frame (1 = no zoom). */
	public float fovDivisor() {
		long now = Util.getMillis();
		// Clamp: the clock can jump (or go backwards in test harnesses), which would break the animation.
		float dt = Math.max(0f, Math.min(0.25f, (now - lastUpdate) / 1000f));
		lastUpdate = now;
		float target = zooming ? (float) scrollLevel : 1f;
		if (!Float.isFinite(current)) {
			current = target;
		}
		if (smooth.isOn()) {
			float speed = 1f - (float) Math.exp(-14f * dt);
			current = Mth.lerp(speed, current, target);
			if (Math.abs(current - target) < 0.002f) {
				current = target;
			}
		} else {
			current = target;
		}
		return Math.max(1f, current);
	}

	/** Mouse look is divided by this while zoomed. */
	public double sensitivityDivisor() {
		return zooming && lowerSensitivity.isOn() ? Math.max(1.0, current) : 1.0;
	}

	/** Returns true if the scroll was used to change the zoom (so the hotbar shouldn't move). */
	public boolean onScroll(double amount) {
		if (!zooming || !scroll.isOn() || amount == 0) {
			return false;
		}
		scrollLevel = Mth.clamp(scrollLevel * (amount > 0 ? 1.15 : 1 / 1.15), 1.0, 50.0);
		return true;
	}
}
