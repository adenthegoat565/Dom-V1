package com.domclient.client.module.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

/** A clickable button inside a module's dropdown. It stores nothing. */
public class ActionSetting extends Setting<Runnable> {
	public ActionSetting(String name, String description, Runnable action) {
		super(name, description, action);
	}

	public void run() {
		value.run();
	}

	@Override
	public String getDisplayValue() {
		return "";
	}

	@Override
	public JsonElement toJson() {
		return JsonNull.INSTANCE;
	}

	@Override
	public void fromJson(JsonElement json) {
	}
}
