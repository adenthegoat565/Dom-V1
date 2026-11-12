package com.domclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;

/** A keyboard key (GLFW key code) that toggles a module. -1 means unbound. */
public class KeybindSetting extends Setting<Integer> {
	public static final int NONE = InputConstants.UNKNOWN.getValue();

	public KeybindSetting(String name, String description, int defaultKey) {
		super(name, description, defaultKey);
	}

	public boolean isBound() {
		return value != NONE;
	}

	@Override
	public String getDisplayValue() {
		if (!isBound()) {
			return "None";
		}
		return InputConstants.Type.KEYSYM.getOrCreate(value).getDisplayName().getString();
	}

	@Override
	public JsonElement toJson() {
		return new JsonPrimitive(value);
	}

	@Override
	public void fromJson(JsonElement json) {
		if (json.isJsonPrimitive() && json.getAsJsonPrimitive().isNumber()) {
			set(json.getAsInt());
		}
	}
}
