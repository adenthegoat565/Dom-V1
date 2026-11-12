package com.domclient.client.integration;

import com.domclient.client.mixin.KeyMappingAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Talks to Simple Voice Chat without hard-depending on it.
 *
 * <p>Read-only state comes from the official client API (captured by {@link DomVoicechatPlugin}).
 * Mute and deafen call the same methods Simple Voice Chat's own keybinds call. If that ever fails,
 * we fall back to "pressing" Simple Voice Chat's key mapping, which it handles once the GUI closes.
 */
public final class VoiceChatBridge {
	private static final Logger LOGGER = LoggerFactory.getLogger("Dom Client/Voice");
	private static final boolean INSTALLED = FabricLoader.getInstance().isModLoaded("voicechat");

	public static final String KEY_MENU = "key.voice_chat";
	public static final String KEY_GROUP = "key.voice_chat_group";
	public static final String KEY_SETTINGS = "key.voice_chat_settings";
	public static final String KEY_VOLUMES = "key.voice_chat_adjust_volumes";
	public static final String KEY_MUTE = "key.mute_microphone";
	public static final String KEY_DISABLE = "key.disable_voice_chat";
	public static final String KEY_HIDE_ICONS = "key.hide_icons";

	/** Set by the plugin. Typed as Object so this class loads fine without Simple Voice Chat. */
	static volatile VoiceState state;

	private static MethodHandle getStateManager;
	private static MethodHandle isMuted;
	private static MethodHandle setMuted;
	private static MethodHandle isDisabled;
	private static MethodHandle setDisabled;
	private static boolean reflectionTried;
	private static boolean reflectionWorks;

	private VoiceChatBridge() {
	}

	/** Snapshot of what the client API reports, filled in by the plugin. */
	interface VoiceState {
		boolean muted();

		boolean disabled();

		boolean disconnected();

		boolean talking();

		boolean whispering();

		String groupName();
	}

	public static boolean isInstalled() {
		return INSTALLED;
	}

	public static boolean isConnected() {
		VoiceState s = state;
		return s != null && !s.disconnected();
	}

	public static boolean isMuted() {
		Boolean viaInternals = readInternal(true);
		if (viaInternals != null) {
			return viaInternals;
		}
		VoiceState s = state;
		return s != null && s.muted();
	}

	public static boolean isDeafened() {
		Boolean viaInternals = readInternal(false);
		if (viaInternals != null) {
			return viaInternals;
		}
		VoiceState s = state;
		return s != null && s.disabled();
	}

	public static boolean isTalking() {
		VoiceState s = state;
		return s != null && (s.talking() || s.whispering());
	}

	public static boolean isWhispering() {
		VoiceState s = state;
		return s != null && s.whispering();
	}

	/** Name of the voice chat group you're in, or null. */
	public static String groupName() {
		VoiceState s = state;
		return s == null ? null : s.groupName();
	}

	public static void setMuted(boolean muted) {
		if (!INSTALLED || isMuted() == muted) {
			return;
		}
		if (!writeInternal(true, muted)) {
			pressKey(KEY_MUTE);
		}
	}

	public static void setDeafened(boolean deafened) {
		if (!INSTALLED || isDeafened() == deafened) {
			return;
		}
		if (!writeInternal(false, deafened)) {
			pressKey(KEY_DISABLE);
		}
	}

	/**
	 * Queues a press of one of Simple Voice Chat's keys. Minecraft only processes key presses
	 * while no screen is open, so callers that open a voice chat screen close our GUI first.
	 */
	public static boolean pressKey(String keyName) {
		if (!INSTALLED) {
			return false;
		}
		KeyMapping mapping = KeyMapping.get(keyName);
		if (mapping == null) {
			LOGGER.warn("Simple Voice Chat key mapping {} not found", keyName);
			return false;
		}
		KeyMappingAccessor accessor = (KeyMappingAccessor) mapping;
		accessor.domclient$setClickCount(accessor.domclient$getClickCount() + 1);
		return true;
	}

	private static synchronized boolean ensureReflection() {
		if (reflectionTried) {
			return reflectionWorks;
		}
		reflectionTried = true;
		try {
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			Class<?> manager = Class.forName("de.maxhenkel.voicechat.voice.client.ClientManager");
			Class<?> stateManager = Class.forName("de.maxhenkel.voicechat.voice.client.ClientPlayerStateManager");
			getStateManager = lookup.findStatic(manager, "getPlayerStateManager", MethodType.methodType(stateManager));
			isMuted = lookup.findVirtual(stateManager, "isMuted", MethodType.methodType(boolean.class));
			setMuted = lookup.findVirtual(stateManager, "setMuted", MethodType.methodType(void.class, boolean.class));
			isDisabled = lookup.findVirtual(stateManager, "isDisabled", MethodType.methodType(boolean.class));
			setDisabled = lookup.findVirtual(stateManager, "setDisabled", MethodType.methodType(void.class, boolean.class));
			reflectionWorks = true;
		} catch (Throwable t) {
			LOGGER.info("Using Simple Voice Chat key presses for mute/deafen ({})", t.toString());
			reflectionWorks = false;
		}
		return reflectionWorks;
	}

	private static Boolean readInternal(boolean muted) {
		if (!INSTALLED || !ensureReflection()) {
			return null;
		}
		try {
			Object manager = getStateManager.invoke();
			if (manager == null) {
				return null;
			}
			return muted ? (boolean) isMuted.invoke(manager) : (boolean) isDisabled.invoke(manager);
		} catch (Throwable t) {
			return null;
		}
	}

	private static boolean writeInternal(boolean muted, boolean value) {
		if (!ensureReflection()) {
			return false;
		}
		try {
			Object manager = getStateManager.invoke();
			if (manager == null) {
				return false;
			}
			if (muted) {
				setMuted.invoke(manager, value);
			} else {
				setDisabled.invoke(manager, value);
			}
			return true;
		} catch (Throwable t) {
			LOGGER.warn("Could not change voice chat state directly", t);
			return false;
		}
	}
}
