package com.domclient.client.module;

import com.domclient.client.config.ConfigManager;
import com.domclient.client.integration.VoiceChatBridge;
import com.domclient.client.module.client.ClickGuiModule;
import com.domclient.client.module.client.HudEditorModule;
import com.domclient.client.module.client.HudSettingsModule;
import com.domclient.client.module.hud.ArmorStatusModule;
import com.domclient.client.module.hud.ClockModule;
import com.domclient.client.module.hud.CoordinatesModule;
import com.domclient.client.module.hud.CpsModule;
import com.domclient.client.module.hud.FpsModule;
import com.domclient.client.module.hud.KeystrokesModule;
import com.domclient.client.module.hud.ModuleListModule;
import com.domclient.client.module.hud.PingModule;
import com.domclient.client.module.hud.SpeedModule;
import com.domclient.client.module.hud.WatermarkModule;
import com.domclient.client.module.player.ToggleSprintModule;
import com.domclient.client.module.render.FullbrightModule;
import com.domclient.client.module.render.ZoomModule;
import com.domclient.client.module.voice.DeafenModule;
import com.domclient.client.module.voice.MuteModule;
import com.domclient.client.module.voice.VoiceScreenModule;
import com.domclient.client.module.voice.VoiceStatusModule;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ModuleManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("Dom Client");
	private static final List<Module> MODULES = new ArrayList<>();
	private static final Map<Class<? extends Module>, Module> BY_CLASS = new HashMap<>();

	private ModuleManager() {
	}

	public static void init() {
		if (!MODULES.isEmpty()) {
			return;
		}
		// HUD
		register(new WatermarkModule());
		register(new ModuleListModule());
		register(new FpsModule());
		register(new CoordinatesModule());
		register(new PingModule());
		register(new CpsModule());
		register(new ClockModule());
		register(new SpeedModule());
		register(new ArmorStatusModule());
		register(new KeystrokesModule());
		register(new HudSettingsModule());
		register(new HudEditorModule());
		// Render
		register(new FullbrightModule());
		register(new ZoomModule());
		// Player
		register(new ToggleSprintModule());
		// Voice chat (Simple Voice Chat)
		register(new MuteModule());
		register(new DeafenModule());
		register(new VoiceStatusModule());
		register(new VoiceScreenModule("Voice Menu", "Open Simple Voice Chat's main menu.", VoiceChatBridge.KEY_MENU));
		register(new VoiceScreenModule("Groups", "Create or join a voice chat group.", VoiceChatBridge.KEY_GROUP));
		register(new VoiceScreenModule("Volumes", "Change how loud each player is.", VoiceChatBridge.KEY_VOLUMES));
		register(new VoiceScreenModule("Voice Settings", "Microphone, speaker and push-to-talk settings.", VoiceChatBridge.KEY_SETTINGS));
		// Client
		register(new ClickGuiModule());
		LOGGER.info("Loaded {} modules", MODULES.size());
	}

	private static void register(Module module) {
		MODULES.add(module);
		BY_CLASS.putIfAbsent(module.getClass(), module);
	}

	@SuppressWarnings("unchecked")
	public static <T extends Module> T get(Class<T> type) {
		return (T) BY_CLASS.get(type);
	}

	public static List<Module> all() {
		return Collections.unmodifiableList(MODULES);
	}

	public static List<Module> byCategory(Category category) {
		List<Module> list = new ArrayList<>();
		for (Module module : MODULES) {
			if (module.getCategory() == category) {
				list.add(module);
			}
		}
		return list;
	}

	public static Module byName(String name) {
		for (Module module : MODULES) {
			if (module.getName().equalsIgnoreCase(name)) {
				return module;
			}
		}
		return null;
	}

	public static void tick() {
		for (Module module : MODULES) {
			try {
				module.onTick();
			} catch (RuntimeException e) {
				LOGGER.error("Module {} crashed while ticking", module.getName(), e);
			}
		}
	}

	/** A key was pressed in-game (no screen open). Runs every module bound to it. */
	public static void onKeyPressed(int key) {
		boolean changed = false;
		for (Module module : MODULES) {
			if (!module.getBind().isBound() || module.getBind().get() != key || !module.isAvailable()) {
				continue;
			}
			module.activate();
			changed = true;
			Minecraft mc = Minecraft.getInstance();
			if (module.getKind() == Module.Kind.TOGGLE && mc.player != null) {
				boolean on = module.isEnabled();
				mc.player.sendOverlayMessage(Component.literal(module.getName() + " ")
					.append(Component.literal(on ? "ON" : "OFF").withStyle(on ? ChatFormatting.GREEN : ChatFormatting.RED)));
			}
		}
		if (changed) {
			ConfigManager.save();
		}
	}
}
