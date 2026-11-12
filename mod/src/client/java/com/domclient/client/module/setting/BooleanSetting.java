package com.domclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

public class BooleanSetting extends Setting<Boolean> {
	public BooleanSetting(String name, String description, boolean defaultValue) {
		super(name, description, defaultValue);
	}

	public boolean isOn() {
		return value;
	}

	public void toggle() {
		set(!value);
	}

	@Override
	public String getDisplayValue() {
		return value ? "On" : "Off";
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isBoolean()) {
			set(json.getAsBoolean());
		}
	}
}
