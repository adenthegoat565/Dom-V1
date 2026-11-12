package com.domclient.client.module.setting;

import com.google.gson.JsonElement;

import java.util.function.BooleanSupplier;

/** A single configurable value that belongs to a module and shows up in its dropdown. */
public abstract class Setting<T> {
	private final String name;
	private final String description;
	private final T defaultValue;
	protected T value;
	private BooleanSupplier visibility = () -> true;

	protected Setting(String name, String description, T defaultValue) {
		this.name = name;
		this.description = description;
		this.defaultValue = defaultValue;
		this.value = defaultValue;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public T get() {
		return value;
	}

	public void set(T value) {
		this.value = value;
	}

	public T getDefault() {
		return defaultValue;
	}

	public void reset() {
		set(defaultValue);
	}

	/** Hides this setting in the GUI unless the condition holds (for example, "only when Rainbow is off"). */
	@SuppressWarnings("unchecked")
	public <S extends Setting<T>> S visibleWhen(BooleanSupplier condition) {
		this.visibility = condition;
		return (S) this;
	}

	public boolean isVisible() {
		return visibility.getAsBoolean();
	}

	/** Text shown on the right side of the row, for example "4.0x" or "Purple". */
	public abstract String getDisplayValue();

	public abstract JsonElement toJson();

	public abstract void fromJson(JsonElement json);
}
