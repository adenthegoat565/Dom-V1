package com.domclient.test;

import com.domclient.client.config.ConfigManager;
import com.domclient.client.gui.ClickGuiScreen;
import com.domclient.client.gui.HudEditorScreen;
import com.domclient.client.integration.VoiceChatBridge;
import com.domclient.client.module.Category;
import com.domclient.client.module.ModuleManager;
import com.domclient.client.module.hud.KeystrokesModule;
import com.domclient.client.module.player.ToggleSprintModule;
import com.domclient.client.module.render.FullbrightModule;
import com.domclient.client.module.render.ZoomModule;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.TestInput;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

/**
 * Drives the real game: opens a world, opens the click GUI with Right Shift, clicks modules,
 * searches, opens the HUD editor and zooms, taking a screenshot after each step.
 */
public class DomClientGameTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("Dom Client test");

	@Override
	public void runTest(ClientGameTestContext context) {
		TestInput input = context.getInput();
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			world.getConnection().waitForChunksRender();
			context.waitTicks(20);
			context.takeScreenshot("01-hud");

			// Right Shift opens the click GUI.
			input.pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
			context.waitForScreen(ClickGuiScreen.class);
			context.waitTicks(10);
			context.takeScreenshot("02-click-gui");

			// Left-click turns modules on.
			clickModule(context, "Fullbright", GLFW.GLFW_MOUSE_BUTTON_LEFT);
			clickModule(context, "Toggle Sprint", GLFW.GLFW_MOUSE_BUTTON_LEFT);
			clickModule(context, "Keystrokes", GLFW.GLFW_MOUSE_BUTTON_LEFT);
			check(context.computeOnClient(mc -> ModuleManager.get(FullbrightModule.class).isEnabled()), "Fullbright should be on");
			check(context.computeOnClient(mc -> ModuleManager.get(ToggleSprintModule.class).isEnabled()), "Toggle Sprint should be on");
			check(context.computeOnClient(mc -> ModuleManager.get(KeystrokesModule.class).isEnabled()), "Keystrokes should be on");

			// Right-click drops down a module's settings.
			clickModule(context, "Zoom", GLFW.GLFW_MOUSE_BUTTON_RIGHT);
			clickModule(context, "Click GUI", GLFW.GLFW_MOUSE_BUTTON_LEFT);
			context.waitTicks(10);
			context.takeScreenshot("03-settings-dropdown");

			// Clicking a header folds the panel up.
			clickPanel(context, Category.PLAYER);
			context.waitTicks(10);
			check(!context.computeOnClient(mc -> ConfigManager.panel(Category.PLAYER).open), "Player panel should be folded");
			clickPanel(context, Category.PLAYER);
			context.waitTicks(10);

			// Typing searches.
			input.typeChars("voice");
			context.waitTicks(10);
			context.takeScreenshot("04-search");
			input.pressKey(GLFW.GLFW_KEY_ESCAPE);
			input.pressKey(GLFW.GLFW_KEY_ESCAPE);
			context.waitFor(mc -> mc.gui.screen() == null);
			check(Files.exists(FabricLoader.getInstance().getConfigDir().resolve("domclient.json")), "config should be saved");

			context.waitTicks(10);
			context.takeScreenshot("05-hud-with-modules");

			context.setScreen(() -> new HudEditorScreen(null));
			context.waitTicks(5);
			context.takeScreenshot("06-hud-editor");
			context.setScreen(() -> null);

			check(context.computeOnClient(mc -> ModuleManager.get(ZoomModule.class).isEnabled()), "Zoom should be on by default");
			float normalFov = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
			input.holdKey(GLFW.GLFW_KEY_C);
			context.waitTicks(15);
			check(context.computeOnClient(mc -> ModuleManager.get(ZoomModule.class).isZooming()), "Holding C should zoom");
			context.takeScreenshot("07-zoom");
			float zoomedFov = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
			LOGGER.info("Field of view: normal {} / zoomed {}", normalFov, zoomedFov);
			check(zoomedFov < normalFov / 2, "Zoom should narrow the field of view (" + normalFov + " -> " + zoomedFov + ")");
			input.releaseKey(GLFW.GLFW_KEY_C);
			context.waitTicks(5);

			boolean voiceInstalled = VoiceChatBridge.isInstalled();
			LOGGER.info("Simple Voice Chat installed: {}, connected: {}", voiceInstalled, VoiceChatBridge.isConnected());
			if (voiceInstalled) {
				// Mute and Deafen flip Simple Voice Chat's own state right away.
				for (String toggle : new String[]{"Mute Mic", "Deafen"}) {
					boolean muteRow = toggle.equals("Mute Mic");
					boolean before = muteRow ? VoiceChatBridge.isMuted() : VoiceChatBridge.isDeafened();
					openGui(context);
					clickModule(context, toggle, GLFW.GLFW_MOUSE_BUTTON_LEFT);
					boolean after = context.computeOnClient(mc -> muteRow ? VoiceChatBridge.isMuted() : VoiceChatBridge.isDeafened());
					LOGGER.info("{}: {} -> {}", toggle, before, after);
					if (!muteRow && before && after) {
						// Simple Voice Chat keeps itself switched off when there's no speaker (CI machines have none).
						LOGGER.info("Voice chat is forced off without an audio device; skipping the Deafen check");
					} else {
						check(after != before, toggle + " should change Simple Voice Chat's state");
					}
					clickModule(context, toggle, GLFW.GLFW_MOUSE_BUTTON_LEFT);
					context.setScreen(() -> null);
				}
				// First button press shows Simple Voice Chat's own setup guide (what a new player sees).
				openGui(context);
				clickModule(context, "Voice Menu", GLFW.GLFW_MOUSE_BUTTON_LEFT);
				context.waitTicks(10);
				String first = context.computeOnClient(mc -> mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
				LOGGER.info("First Voice Menu press opened: {}", first);
				context.takeScreenshot("08-voice-setup");
				context.setScreen(() -> null);
				context.waitTicks(2);
				// Pretend the player finished that guide, then every button should open its own screen.
				context.runOnClient(mc -> finishVoiceChatSetup());
				int shot = 9;
				for (String button : new String[]{"Voice Menu", "Groups", "Volumes", "Voice Settings"}) {
					openGui(context);
					clickModule(context, button, GLFW.GLFW_MOUSE_BUTTON_LEFT);
					context.waitTicks(10);
					String screen = context.computeOnClient(mc -> mc.gui.screen() == null ? "none" : mc.gui.screen().getClass().getSimpleName());
					LOGGER.info("{} button opened: {}", button, screen);
					if (!button.equals("Groups")) {
						// Groups needs a working voice connection, which CI machines (no audio device) don't get.
						check(!screen.equals("none") && !screen.equals("ClickGuiScreen") && !screen.contains("Onboarding"),
							button + " should open Simple Voice Chat's own screen, got " + screen);
					}
					context.takeScreenshot(String.format("%02d-%s", shot++, button.toLowerCase().replace(' ', '-')));
					context.setScreen(() -> null);
					context.waitTicks(2);
				}
			}
		}
		LOGGER.info("Dom Client in-game test passed");
	}

	/** Sets Simple Voice Chat's "onboarding finished" option, like completing its first-time setup. */
	private static void finishVoiceChatSetup() {
		try {
			Class<?> client = Class.forName("de.maxhenkel.voicechat.VoicechatClient");
			Object config = client.getField("CLIENT_CONFIG").get(null);
			Object entry = config.getClass().getField("onboardingFinished").get(config);
			Class.forName("de.maxhenkel.voicechat.configbuilder.entry.ConfigEntry").getMethod("set", Object.class).invoke(entry, Boolean.TRUE);
		} catch (ReflectiveOperationException e) {
			throw new AssertionError("Couldn't finish Simple Voice Chat setup", e);
		}
	}

	private static void openGui(ClientGameTestContext context) {
		context.getInput().pressKey(GLFW.GLFW_KEY_RIGHT_SHIFT);
		context.waitForScreen(ClickGuiScreen.class);
		context.waitTicks(5);
	}

	private static void check(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static void clickModule(ClientGameTestContext context, String name, int button) {
		int[] pos = context.computeOnClient(mc -> mc.gui.screen() instanceof ClickGuiScreen gui ? gui.locateModule(name) : null);
		if (pos == null) {
			throw new AssertionError("Module row not visible: " + name);
		}
		click(context, pos[0], pos[1], button);
	}

	private static void clickPanel(ClientGameTestContext context, Category category) {
		int[] pos = context.computeOnClient(mc -> ((ClickGuiScreen) mc.gui.screen()).locatePanel(category));
		click(context, pos[0], pos[1], GLFW.GLFW_MOUSE_BUTTON_LEFT);
	}

	private static void click(ClientGameTestContext context, int guiX, int guiY, int button) {
		double scale = context.computeOnClient(mc -> mc.getWindow().getGuiScale());
		context.getInput().setCursorPos(guiX * scale, guiY * scale);
		context.waitTick();
		context.getInput().pressMouse(button);
		context.waitTicks(3);
	}
}
