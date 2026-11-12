package com.domclient.client.module;

import com.domclient.client.module.setting.KeybindSetting;
import com.domclient.client.module.setting.Setting;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class Module {
	/** How a module reacts when its row in the click GUI is left-clicked. */
	public enum Kind {
		/** Turns on and off. */
		TOGGLE,
		/** Runs {@link #onAction()} once, like a button. */
		ACTION,
		/** Only holds settings; clicking opens the dropdown. */
		SETTINGS
	}

	protected final Minecraft mc = Minecraft.getInstance();
	private final String name;
	private final String description;
	private final Category category;
	private final Kind kind;
	private final List<Setting<?>> settings = new ArrayList<>();
	private final KeybindSetting bind;
	private boolean enabled;

	protected Module(String name, String description, Category category) {
		this(name, description, category, Kind.TOGGLE);
	}

	protected Module(String name, String description, Category category, Kind kind) {
		this.name = name;
		this.description = description;
		this.category = category;
		this.kind = kind;
		this.bind = new KeybindSetting("Bind", "Key that toggles this module. Esc clears it.", KeybindSetting.NONE);
	}

	protected <T extends Setting<?>> T add(T setting) {
		settings.add(setting);
		return setting;
	}

	/** Adds the keybind row. Toggle and action modules get one; settings-only modules don't need it. */
	protected void addBindSetting() {
		settings.add(bind);
	}

	public final String getName() {
		return name;
	}

	public final String getDescription() {
		return description;
	}

	public final Category getCategory() {
		return category;
	}

	public final Kind getKind() {
		return kind;
	}

	public List<Setting<?>> getSettings() {
		return Collections.unmodifiableList(settings);
	}

	public KeybindSetting getBind() {
		return bind;
	}

	public boolean isEnabled() {
		return enabled;
	}

	/** Greyed out in the GUI (for example the voice chat modules when Simple Voice Chat isn't installed). */
	public boolean isAvailable() {
		return true;
	}

	public String getUnavailableReason() {
		return "";
	}

	public void setEnabled(boolean enabled) {
		if (kind != Kind.TOGGLE || this.enabled == enabled) {
			return;
		}
		this.enabled = enabled;
		if (enabled) {
			onEnable();
		} else {
			onDisable();
		}
	}

	/** Restores the saved on/off state without side effects that need a running game. */
	public void loadEnabled(boolean enabled) {
		if (kind == Kind.TOGGLE) {
			this.enabled = enabled;
		}
	}

	public void toggle() {
		setEnabled(!isEnabled());
	}

	/** What the bind key and a left click do. */
	public void activate() {
		switch (kind) {
			case TOGGLE -> toggle();
			case ACTION -> onAction();
			case SETTINGS -> {
			}
		}
	}

	protected void onEnable() {
	}

	protected void onDisable() {
	}

	protected void onAction() {
	}

	/** Called every client tick while the module is enabled (or always, for action/settings modules). */
	public void onTick() {
	}

	/** Extra text shown after the name in the module list HUD, for example "4.0x". */
	public String getInfo() {
		return "";
	}

	/** Whether the module list HUD should show this module while it's on. */
	public boolean showInModuleList() {
		return kind == Kind.TOGGLE;
	}
}
