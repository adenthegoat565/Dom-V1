package com.domclient.client.integration;

import de.maxhenkel.voicechat.api.Group;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatClientApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientVoicechatConnectionEvent;
import de.maxhenkel.voicechat.api.events.ClientVoicechatInitializationEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;

/**
 * Simple Voice Chat plugin entrypoint ("voicechat" in fabric.mod.json). Only loaded by Simple Voice Chat
 * itself, so referencing its API classes here is safe even when it isn't installed.
 */
public class DomVoicechatPlugin implements VoicechatPlugin {
	@Override
	public String getPluginId() {
		return "domclient";
	}

	@Override
	public void initialize(VoicechatApi api) {
		if (api instanceof VoicechatClientApi client) {
			capture(client);
		}
	}

	@Override
	public void registerEvents(EventRegistration registration) {
		registration.registerEvent(ClientVoicechatInitializationEvent.class, event -> capture(event.getVoicechat()));
		registration.registerEvent(ClientVoicechatConnectionEvent.class, event -> capture(event.getVoicechat()));
	}

	private static void capture(VoicechatClientApi api) {
		if (api == null) {
			return;
		}
		VoiceChatBridge.state = new VoiceChatBridge.VoiceState() {
			@Override
			public boolean muted() {
				return api.isMuted();
			}

			@Override
			public boolean disabled() {
				return api.isDisabled();
			}

			@Override
			public boolean disconnected() {
				return api.isDisconnected();
			}

			@Override
			public boolean talking() {
				return api.isTalking();
			}

			@Override
			public boolean whispering() {
				return api.isWhispering();
			}

			@Override
			public String groupName() {
				Group group = api.getGroup();
				return group == null ? null : group.getName();
			}
		};
	}
}
