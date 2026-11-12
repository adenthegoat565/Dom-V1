package com.domclient.client.module;

public enum Category {
	HUD("HUD"),
	RENDER("Render"),
	PLAYER("Player"),
	VOICE("Voice Chat"),
	CLIENT("Client");

	private final String displayName;

	Category(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
