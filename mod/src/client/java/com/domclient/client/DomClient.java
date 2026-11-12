package com.domclient.client;

import com.domclient.client.config.ConfigManager;
import com.domclient.client.gui.ClickGuiScreen;
import com.domclient.client.gui.HudRenderer;
import com.domclient.client.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DomClient implements ClientModInitializer {
	public static final String MOD_ID = "domclient";
	public static final Logger LOGGER = LoggerFactory.getLogger("Dom Client");
	public static final String VERSION = FabricLoader.getInstance().getModContainer(MOD_ID)
		.map(mod -> mod.getMetadata().getVersion().getFriendlyString())
		.orElse("dev");

	private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));

	/** Opens the click GUI. Right Shift by default; players can change it in Options > Controls. */
	public static final KeyMapping CLICK_GUI_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.domclient.click_gui", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));

	/** Hold to zoom (when the Zoom module is on). */
	public static final KeyMapping ZOOM_KEY = KeyMappingHelper.registerKeyMapping(
		new KeyMapping("key.domclient.zoom", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_C, CATEGORY));

	@Override
	public void onInitializeClient() {
		ModuleManager.init();
		ConfigManager.load();

		HudElementRegistry.addLast(Identifier.fromNamespaceAndPath(MOD_ID, "hud"), HudRenderer::render);

		ClientTickEvents.END_CLIENT_TICK.register(DomClient::onEndTick);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> ConfigManager.save());
		LOGGER.info("Dom Client {} ready - press Right Shift in game to open the click GUI", VERSION);
	}

	private static void onEndTick(Minecraft client) {
		while (CLICK_GUI_KEY.consumeClick()) {
			if (client.gui.screen() == null && client.player != null) {
				client.gui.setScreen(new ClickGuiScreen());
			}
		}
		ModuleManager.tick();
	}
}
