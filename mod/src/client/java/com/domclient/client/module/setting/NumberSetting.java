package com.domclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.Locale;

public class NumberSetting extends Setting<Double> {
	private final double min;
	private final double max;
	private final double step;
	private final String suffix;

	public NumberSetting(String name, String description, double defaultValue, double min, double max, double step, String suffix) {
		super(name, description, defaultValue);
		this.min = min;
		this.max = max;
		this.step = step;
		this.suffix = suffix;
	}

	@Override
	public void set(Double value) {
		double clamped = Math.max(min, Math.min(max, value));
		if (step > 0) {
			clamped = Math.round((clamped - min) / step) * step + min;
			clamped = Math.max(min, Math.min(max, clamped));
		}
		super.set(clamped);
	}

	public double getMin() {
		return min;
	}

	public double getMax() {
		return max;
	}

	public double getStep() {
		return step;
	}

	public float getFloat() {
		return value.floatValue();
	}

	public int getInt() {
		return (int) Math.round(value);
	}

	/** 0..1 position of the current value inside the range, used to draw the slider. */
	public double getProgress() {
		return max == min ? 0 : (value - min) / (max - min);
	}

	public void setFromProgress(double progress) {
		set(min + (max - min) * Math.max(0, Math.min(1, progress)));
	}

	@Override
	public String getDisplayValue() {
		boolean whole = step >= 1 && Math.abs(step - Math.rint(step)) < 1e-9;
		String number = whole ? Integer.toString(getInt()) : String.format(Locale.ROOT, "%.1f", value);
		return number + suffix;
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsDouble());
		}
	}
}
