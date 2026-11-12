package com.domclient.client.config;

import com.domclient.client.module.Category;
import com.domclient.client.module.Module;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.hud.HudModule;
import com.domclient.client.module.setting.Setting;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;

/** Saves module states, settings, binds, HUD positions and panel positions to config/domclient.json. */
public final class ConfigManager {
	private static final Logger LOGGER = LoggerFactory.getLogger("Dom Client/Config");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Map<Category, PanelState> PANELS = new EnumMap<>(Category.class);

	private ConfigManager() {
	}

	/** Where a click GUI panel sits and whether its dropdown is open. x/y below 0 mean "not placed yet". */
	public static final class PanelState {
		public float x = -1;
		public float y = -1;
		public boolean open = true;
	}

	public static PanelState panel(Category category) {
		return PANELS.computeIfAbsent(category, c -> new PanelState());
	}

	public static void resetPanels() {
		for (Category category : Category.values()) {
			PanelState state = panel(category);
			state.x = -1;
			state.y = -1;
			state.open = true;
		}
		save();
	}

	private static Path file() {
		return FabricLoader.getInstance().getConfigDir().resolve("domclient.json");
	}

	public static void load() {
		Path path = file();
		if (!Files.exists(path)) {
			return;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8)).getAsJsonObject();
			JsonObject modules = root.has("modules") ? root.getAsJsonObject("modules") : new JsonObject();
			for (Module module : ModuleManager.all()) {
				if (!modules.has(module.getName())) {
					continue;
				}
				JsonObject obj = modules.getAsJsonObject(module.getName());
				if (obj.has("enabled")) {
					module.loadEnabled(obj.get("enabled").getAsBoolean());
				}
				if (obj.has("bind")) {
					module.getBind().fromJson(obj.get("bind"));
				}
				JsonObject settings = obj.has("settings") ? obj.getAsJsonObject("settings") : new JsonObject();
				for (Setting<?> setting : module.getSettings()) {
					JsonElement value = settings.get(setting.getName());
					if (value != null && setting != module.getBind()) {
						try {
							setting.fromJson(value);
						} catch (RuntimeException e) {
							LOGGER.warn("Ignoring bad value for {} / {}", module.getName(), setting.getName());
						}
					}
				}
				if (module instanceof HudModule hud && obj.has("hud")) {
					JsonObject pos = obj.getAsJsonObject("hud");
					hud.setCustomPosition(pos.get("x").getAsFloat(), pos.get("y").getAsFloat());
				}
			}
			JsonObject panels = root.has("panels") ? root.getAsJsonObject("panels") : new JsonObject();
			for (Category category : Category.values()) {
				if (panels.has(category.name())) {
					JsonObject obj = panels.getAsJsonObject(category.name());
					PanelState state = panel(category);
					state.x = obj.get("x").getAsFloat();
					state.y = obj.get("y").getAsFloat();
					state.open = obj.get("open").getAsBoolean();
				}
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.error("Could not read {}, using defaults", path, e);
		}
	}

	public static synchronized void save() {
		JsonObject root = new JsonObject();
		root.addProperty("version", 1);
		JsonObject modules = new JsonObject();
		for (Module module : ModuleManager.all()) {
			JsonObject obj = new JsonObject();
			if (module.getKind() == Module.Kind.TOGGLE) {
				obj.addProperty("enabled", module.isEnabled());
			}
			obj.add("bind", module.getBind().toJson());
			JsonObject settings = new JsonObject();
			for (Setting<?> setting : module.getSettings()) {
				if (setting != module.getBind() && !setting.toJson().isJsonNull()) {
					settings.add(setting.getName(), setting.toJson());
				}
			}
			obj.add("settings", settings);
			if (module instanceof HudModule hud && hud.hasCustomPosition()) {
				JsonObject pos = new JsonObject();
				pos.addProperty("x", hud.getRelX());
				pos.addProperty("y", hud.getRelY());
				obj.add("hud", pos);
			}
			modules.add(module.getName(), obj);
		}
		root.add("modules", modules);
		JsonObject panels = new JsonObject();
		for (Map.Entry<Category, PanelState> entry : PANELS.entrySet()) {
			JsonObject obj = new JsonObject();
			obj.addProperty("x", entry.getValue().x);
			obj.addProperty("y", entry.getValue().y);
			obj.addProperty("open", entry.getValue().open);
			panels.add(entry.getKey().name(), obj);
		}
		root.add("panels", panels);

		Path path = file();
		try {
			Files.createDirectories(path.getParent());
			Path tmp = path.resolveSibling("domclient.json.tmp");
			Files.writeString(tmp, GSON.toJson(root), StandardCharsets.UTF_8);
			Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} catch (IOException e) {
			try {
				Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
			} catch (IOException e2) {
				LOGGER.error("Could not save {}", path, e2);
			}
		}
	}
}
